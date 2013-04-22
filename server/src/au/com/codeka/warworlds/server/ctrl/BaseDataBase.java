package au.com.codeka.warworlds.server.ctrl;

/**
 * This is our base "database" class that includes some common methods for working with the database.
 */
public class BaseDataBase {
    protected static String buildInClause(int[] ids) {
        StringBuffer sb = new StringBuffer();
        sb.append("(");
        for (int i = 0; i < ids.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(ids[i]);
        }
        sb.append(")");
        return sb.toString();
    }
}
