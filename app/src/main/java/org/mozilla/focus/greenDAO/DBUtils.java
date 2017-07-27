package org.mozilla.focus.greenDAO;

/**
 * Created by anlin on 27/07/2017.
 */

public class DBUtils {

    private static DBService dbService;

    public static DBService getDbService(){
        if (dbService == null){
            dbService = new DBService();
        }
        return dbService;
    }
}
