package au.com.codeka.warworlds.client.net.auth

import com.google.android.gms.auth.api.signin.GoogleSignInAccount

/**
 * This event is published on the event bus when the auth account is updated. It could be null
 * if you're not signed in or it could have an account.
 */
data class AuthAccountUpdate(val account: GoogleSignInAccount?)
