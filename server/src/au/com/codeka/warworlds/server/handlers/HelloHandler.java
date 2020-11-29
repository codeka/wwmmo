package au.com.codeka.warworlds.server.handlers;

import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.json.webtoken.JsonWebSignature;
import com.google.common.io.Files;

import org.apache.http.conn.ssl.DefaultHostnameVerifier;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.util.List;

import javax.annotation.Nullable;
import javax.net.ssl.SSLException;

import au.com.codeka.common.Log;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.common.safetynet.ValidationFailureException;
import au.com.codeka.common.safetynet.ValidationFailureReason;
import au.com.codeka.warworlds.server.Configuration;
import au.com.codeka.warworlds.server.FeatureFlags;
import au.com.codeka.warworlds.server.RequestContext;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.RequestHandler;
import au.com.codeka.warworlds.server.Session;
import au.com.codeka.warworlds.server.ctrl.EmpireController;
import au.com.codeka.warworlds.server.ctrl.GameHistoryController;
import au.com.codeka.warworlds.server.ctrl.LoginController;
import au.com.codeka.warworlds.server.ctrl.StatisticsController;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlResult;
import au.com.codeka.warworlds.server.data.SqlStmt;
import au.com.codeka.warworlds.server.model.Empire;
import au.com.codeka.warworlds.server.model.GameHistory;
import au.com.codeka.warworlds.server.model.Star;
import au.com.codeka.warworlds.server.utils.SafetyNetAttestationStatement;

public class HelloHandler extends RequestHandler {
  private final Log log = new Log("HelloHandler");

  private static final DefaultHostnameVerifier hostnameVerifier = new DefaultHostnameVerifier();

  // Our min-version, initialized on first request.
  private static int[] minVersion = null;
  private static String minVersionStr = null;

  private static String serverVersionStr = null;

  @Override
  protected void get() throws RequestException {
    // this is just used for testing, nothing more...
    if (!getSession().isAdmin()) {
      throw new RequestException(501);
    }

    Messages.HelloRequest hello_request_pb = Messages.HelloRequest.newBuilder()
        .setAllowInlineNotfications(false)
        .setDeviceBuild("TEST_DEVICE_BUILD")
        .setDeviceManufacturer("TEST_DEVICE_MANUFACTURER")
        .setDeviceModel("TEST_DEVICE_MODEL")
        .setDeviceVersion("TEST_DEVICE_VERSION")
        .setMemoryClass(0)
        .build();
    processHello(hello_request_pb);
  }

  @Override
  protected void put() throws RequestException {
    processHello(getRequestBody(Messages.HelloRequest.class));
  }

  private void processHello(Messages.HelloRequest hello_request_pb) throws RequestException {
    long startTime = System.currentTimeMillis();

    Messages.HelloResponse.Builder hello_response_pb = Messages.HelloResponse.newBuilder()
        .setMotd(Messages.MessageOfTheDay.newBuilder().setMessage("").setLastUpdate(""))
        .setServerVersion(getServerVersion())
        .setFeatureFlags(FeatureFlags.get());

    final String userAgent = getRequest().getHeader("User-Agent");

    GameHistory gameHistory = new GameHistoryController().getCurrent();
    if (gameHistory == null) {
      log.info("No game history, game is possibly currently resetting?");
      throw new RequestException(
          400,
          Messages.GenericError.ErrorCode.ResetInProgress,
          "Cannot log in yet, game is resetting.");
    }

    SafetyNetAttestationStatement attestationStatement = null;
    if (hello_request_pb.hasSafetynetJwsResult()) {
      try {
        attestationStatement = validateSafetyNetJws(hello_request_pb);
      } catch (ValidationFailureException e) {
        log.warning("SafetyNet attestation validation failed.", e);
      }
    }

    // fetch the empire we're interested in
    Empire empire = new EmpireController().getEmpire(getSession().getEmpireID());
    if (empire != null) {
      try {
        ensureMinVersion(userAgent);
        ensureNoClickers(hello_request_pb.getAccessibilitySettingsInfo());
        ensureClientId(getSession());
        ensureSafetyNetAttestation(empire.getID(), attestationStatement);
        ensureDeviceDeniedStatus(empire.getID(), hello_request_pb);
      } catch (RequestException e) {
        log.error("Recording failed login");
        new StatisticsController().registerFailedLogin(
            getSession(), getRequest().getHeader("User-Agent"), hello_request_pb,
            attestationStatement, e.getMessage());
        throw e;
      }

      new StatisticsController().registerLogin(
          getSession(), getRequest().getHeader("User-Agent"), hello_request_pb,
          attestationStatement);
      if (empire.getState() == Empire.State.ABANDONED) {
        new EmpireController().markActive(empire);
      }

      // Make sure the session is up-to-date with things like the empire's alliance etc.
      new LoginController().updateSession(getSession());

      log.info("Login in %dms %d [%s] [%s] [%s] [%s]\nSafetyNet: %s",
          System.currentTimeMillis() - startTime,
          empire.getID(),
          empire.getDisplayName(),
          getSession().getEmail(),
          RequestContext.i.getIpAddress(),
          userAgent,
          attestationStatement);

      // Make sure they haven't been wiped out.
      if (!new EmpireController().hasAnyColonies(getSession().getEmpireID())) {
        log.info(
            "Empire #%d [%s] has been wiped out, resetting.",
            empire.getID(), empire.getDisplayName());
        new EmpireController().createEmpire(empire);
        hello_response_pb.setWasEmpireReset(true);

        String resetReason = new EmpireController().getResetReason(empire.getID());
        if (resetReason != null) {
          hello_response_pb.setEmpireResetReason(resetReason);
        }
      }

      Messages.Empire.Builder empire_pb = Messages.Empire.newBuilder();
      empire.toProtocolBuffer(empire_pb, true);
      hello_response_pb.setEmpire(empire_pb);

      // set up the initial building statistics
      String sql =
          "SELECT design_id, COUNT(*) FROM buildings WHERE empire_id = ? GROUP BY design_id";
      try (SqlStmt stmt = DB.prepare(sql)) {
        stmt.setInt(1, empire.getID());
        SqlResult res = stmt.select();

        Messages.EmpireBuildingStatistics.Builder build_stats_pb =
            Messages.EmpireBuildingStatistics.newBuilder();
        while (res.next()) {
          String designID = res.getString(1);
          int num = res.getInt(2);

          Messages.EmpireBuildingStatistics.DesignCount.Builder design_count_pb =
              Messages.EmpireBuildingStatistics.DesignCount.newBuilder();
          design_count_pb.setDesignId(designID);
          design_count_pb.setNumBuildings(num);
          build_stats_pb.addCounts(design_count_pb);
        }
        hello_response_pb.setBuildingStatistics(build_stats_pb);
      } catch (Exception e) {
        throw new RequestException(e);
      }

      // if we're set to force ignore ads, make sure we pass that along
      hello_response_pb.setForceRemoveAds(empire.getForceRemoveAds());

      if (!hello_request_pb.hasNoStarList() || !hello_request_pb.getNoStarList()) {
        // grab all of the empire's stars (except markers and wormholes) and send across the
        // identifiers
        sql = "SELECT id, name" +
            " FROM stars" +
            " INNER JOIN (SELECT DISTINCT star_id FROM colonies WHERE empire_id = ?" +
            " UNION SELECT DISTINCT star_id FROM fleets WHERE empire_id = ?) as s" +
            " ON s.star_id = stars.id" +
            " WHERE star_type NOT IN (" + Star.Type.Marker.ordinal() + ", " + Star.Type.Wormhole
            .ordinal() + ")" +
            " ORDER BY name ASC";
        try (SqlStmt stmt = DB.prepare(sql)) {
          stmt.setInt(1, empire.getID());
          stmt.setInt(2, empire.getID());
          SqlResult res = stmt.select();

          while (res.next()) {
            hello_response_pb.addStarIds(res.getLong(1));
          }
        } catch (Exception e) {
          throw new RequestException(e);
        }
      }
    }

    setResponseBody(hello_response_pb.build());
  }

  private void ensureClientId(Session session) throws RequestException {
    if (session.isAnonymous()) {
      // Anonymous users don't log in, so we won't get the client ID.
      return;
    }

    if (Configuration.i.getAllowNonProdClientLogins()) {
      // We allow non-prod client to connect, so this one is OK.
      return;
    }

    if (!session.getClientId().equals(Configuration.PROD_CLIENT_ID)) {
      String clientId = session.getClientId();
      if (clientId.equals(Configuration.DEV_CLIENT_ID)) {
        clientId = "DEV";
      }
      log.warning("Connection from non-prod client (%s), rejecting.", clientId);
      throw new RequestException(
          400, Messages.GenericError.ErrorCode.UpgradeRequired,
          "Please use the official app downloaded from the Play Store.");
    }
  }

  /**
   * Parses and validates the integrity of the SafetyNet attestation. This method doesn't verify
   * the contents of the attestation (that's what
   * {@link #ensureSafetyNetAttestation(int, SafetyNetAttestationStatement)} is for).
   */
  private SafetyNetAttestationStatement validateSafetyNetJws(
      Messages.HelloRequest helloRequest) throws ValidationFailureException {
    if (!helloRequest.hasSafetynetJwsResult()) {
      throw new ValidationFailureException(ValidationFailureReason.JSW_MISSING);
    }
    if (helloRequest.getSafetynetJwsResult().startsWith("ERROR:")) {
      log.error(
          "SafetyNet JSW results indicates a client-side error: %s",
          helloRequest.getSafetynetJwsResult());
      throw new ValidationFailureException(
          ValidationFailureReason.JSW_MISSING, helloRequest.getSafetynetJwsResult());
    }

    JsonWebSignature jws;
    try {
      jws = JsonWebSignature.parser(GsonFactory.getDefaultInstance())
          .setPayloadClass(SafetyNetAttestationStatement.class)
          .parse(helloRequest.getSafetynetJwsResult());
    } catch (IOException e) {
      log.error("Error parsing SafetyNet JWS.", e);
      throw new ValidationFailureException(ValidationFailureReason.INVALID_JWS, e);
    }

    X509Certificate cert;
    try {
      cert = jws.verifySignature();
    } catch (GeneralSecurityException e) {
      log.error("Exception while verifying signature in SafetyNet JWS attestation.", e);
      throw new ValidationFailureException(ValidationFailureReason.JWS_SIGNATURE_ERROR, e);
    }

    try {
      // This throws an exception if the hostname doesn't match.
      hostnameVerifier.verify("attest.android.com", cert);
    } catch (SSLException e) {
      log.error("Exception while verifying hostname of attestation", e);
      throw new ValidationFailureException(
          ValidationFailureReason.JWS_SIGNATURE_HOSTNAME_MISMATCH, e);
    }

    // Now that we have a valid attestation statement, verify the contents of it.
    SafetyNetAttestationStatement attestationStatement =
        (SafetyNetAttestationStatement) jws.getPayload();
    // TODO: verify.

    return attestationStatement;
  }

  /**
   * Checks that either this empire is exempt from SafetyNet attestation, or that they pass.
   */
  private void ensureSafetyNetAttestation(
      int empireId, @Nullable SafetyNetAttestationStatement attestationStatement)
      throws RequestException {
    Configuration.SafetyNetConfig config = Configuration.i.getSafetyNet();
    if (config == null || !config.isEnabled()) {
      log.debug("SafetyNet is disabled, not checking.");
      return;
    }
    log.debug("SafetyNet is enabled.");

    if (config.getExemptedEmpires() != null) {
      for (int i = 0; i < config.getExemptedEmpires().length; i++) {
        if (empireId == config.getExemptedEmpires()[i]) {
          log.debug("Empire [%d] is exempt from SafetyNet.", empireId);
          return;
        }
      }
    }
    log.debug("Empire [%d] is not exempt from SafetyNet checked.");

    boolean pass = true;
    if (attestationStatement == null) {
      log.warning("Not attestation statement for empire [%d], rejecting connection", empireId);
      pass = false;
    } else if (!attestationStatement.hasBasicIntegrity()) {
      log.warning(
          "SafetyNet attestation fails basic integrity check for empire [%d], rejecting connection",
          empireId);
      pass = false;
    }

    if (!pass) {
      throw new RequestException(400, Messages.GenericError.ErrorCode.ClientDeviceRejected,
          "You're running an unsupported device.\nIf you believe this to be in error, please " +
              "contact me via discord.")
          .withSkipLog();
    }
  }

  /**
   * Check the list of accessibility services the client has provided us, and reject the request if
   * any of them are in our configured list of unsupported ones.
   */
  private void ensureNoClickers(
      Messages.AccessibilitySettingsInfo accessibilitySettingsInfo) throws RequestException {
    if (!accessibilitySettingsInfo.getSupported()) {
      log.debug("Client has no accessibility services enabled.");
      return;
    }

    Configuration.ClickerConfig[] clickers = Configuration.i.getClickers();
    if (clickers == null) {
      log.debug("No clickers configured, nothing to check.");
      return;
    }

    for (Messages.AccessibilitySettingsInfo.AccessibilityService service :
        accessibilitySettingsInfo.getServiceList()) {
      for (Configuration.ClickerConfig clicker : clickers) {
        if (clicker.getName().equalsIgnoreCase(service.getName())) {
          log.warning(
              "User has unsupported accessibility service installed: %s",
              clicker.getName());
          throw new RequestException(
              400,
              Messages.GenericError.ErrorCode.ClientDeviceRejected,
              String.format("Cannot connect while you have '%s' installed.\nPlease disable or " +
                  "uninstall it and try again.", clicker.getAppName()))
              .withLogMessageOnly();
        }
      }
    }
  }

  private void ensureDeviceDeniedStatus(
      int empireId, Messages.HelloRequest hello_request_pb) throws RequestException {
    if (Configuration.i.getSafetyNet() == null || !Configuration.i.getSafetyNet().isEnabled()) {
      // SafetyNet is disabled, so ignore whether this device was denied before.
      return;
    }

    if (new EmpireController().isDeviceAccessDenied(
        empireId, hello_request_pb.getDeviceModel(), hello_request_pb.getDeviceBuild())) {
      log.debug("Device set as access denied, rejecting connection.");
      throw new RequestException(
          400,
          Messages.GenericError.ErrorCode.ClientDeviceRejected,
          "You are running an unsupported device.\nIf you believe this to be in error, please " +
              "contact me via discord.")
          .withLogMessageOnly();
    }
  }

  /**
   * Ensures the the User-Agent given specified a version for the client that we support.
   *
   * The client sends a User-Agent of the form "wwmmo/(version)" where version will be something
   * like "1.2.345". We want to make sure that a) it's in the correct format and b) that it has
   * a version number that's greater than what is specified in min-version.ini.
   *
   * @param userAgent A User-Agent string, in the form "wwmmo/a.b.c".
   * @throws RequestException if the min version doesn't match.
   */
  private void ensureMinVersion(String userAgent) throws RequestException {
    String[] parts = userAgent.split("/");
    if (parts.length != 2 && !parts[0].equals("wwmmo")) {
      log.warning("User-Agent doesn't match expected format: %s", userAgent);
      throw new RequestException(400, "Unsupported client");
    }
    String version = parts[1];

    synchronized (HelloHandler.class) {
      if (minVersion == null) {
        try {
          File minVersionFile = new File(Configuration.i.getConfigDirectory(), "min-version.ini");
          List<String> lines = Files.readLines(minVersionFile, Charset.defaultCharset());
          for (String line : lines) {
            if (line.trim().length() > 0) {
              minVersionStr = line.trim();
              parts = line.trim().split("\\.");
              minVersion = new int[parts.length];
              for (int i = 0; i < parts.length; i++) {
                minVersion[i] = Integer.parseInt(parts[i]);
              }
              log.debug("Parsed min-version.ini: '%s' %s", minVersionStr, minVersion.length);
            }
          }
        } catch (IOException e) {
          throw new RequestException(e);
        }
      }
    }

    parts = version.split("\\.");
    if (parts.length != minVersion.length) {
      log.warning("User-Agent's version is not in the correct format: %s", userAgent);
      throw new RequestException(400, "Incorrect version number");
    }

    // Go through the parts in order. Any number that's bigger than what's in min-version.ini means
    // this client is an acceptable version.
    for (int i = 0; i < parts.length; i++) {
      int part;
      try {
        part = Integer.parseInt(parts[i]);
      } catch (NumberFormatException e) {
        log.warning("User-Agent's version contains non-numbers: %s", userAgent);
        throw new RequestException(400, "Incorrect version number");
      }

      if (part < minVersion[i]) {
        log.warning(
            "User-Agent's version number is too low: %s (min-version: %s)",
            userAgent,
            minVersionStr);
        throw new RequestException(
            426,
            Messages.GenericError.ErrorCode.UpgradeRequired,
            "Upgrade required.\\nPlease update from the Play Store.");
      }
    }

    log.debug("Supported version number: %s (min-version: %s)", userAgent, minVersionStr);
  }

  private static String getServerVersion() {
    if (serverVersionStr == null) {
      serverVersionStr = HelloHandler.class.getPackage().getImplementationVersion();
    }
    return serverVersionStr;
  }
}
