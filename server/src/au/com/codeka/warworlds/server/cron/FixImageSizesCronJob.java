package au.com.codeka.warworlds.server.cron;

import java.awt.image.BufferedImage;
import java.sql.ResultSet;
import java.util.Locale;

import org.apache.commons.imaging.Imaging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlStmt;
import au.com.codeka.warworlds.server.utils.ImageSizer;

public class FixImageSizesCronJob extends CronJob {
    private static final Logger log = LoggerFactory.getLogger(FixImageSizesCronJob.class);

    @Override
    public void run(String extra) throws Exception {
        String sql = "SELECT * FROM alliance_requests WHERE png_image IS NOT NULL";
        try (SqlStmt stmt = DB.prepare(sql)) {
            ResultSet rs = stmt.select();
            while (rs.next()) {
                byte[] pngImage = rs.getBytes("png_image");
                BufferedImage oldImage = Imaging.getBufferedImage(pngImage);
                pngImage = ImageSizer.ensureMaxSize(pngImage, 128, 128);
                BufferedImage newImage = Imaging.getBufferedImage(pngImage);

                if (oldImage.getWidth() == newImage.getWidth()) {
                    continue;
                }
                log.info(String.format(Locale.ENGLISH,
                        "Image for request #%d resized from %dx%d to %dx%d (ratio before=%.2f after=%.2f).",
                        rs.getInt("id"), oldImage.getWidth(), oldImage.getHeight(),
                        newImage.getWidth(), newImage.getHeight(),
                        (float) oldImage.getWidth() / oldImage.getHeight(),
                        (float) newImage.getWidth() / newImage.getHeight()));

                sql = "UPDATE alliance_requests SET png_image = ? WHERE id = ?";
                try (SqlStmt innerstmt = DB.prepare(sql)) {
                    innerstmt.setBytes(1, pngImage);
                    innerstmt.setInt(2, rs.getInt("id"));
                    innerstmt.update();
                }
            }
        }
    }
}
