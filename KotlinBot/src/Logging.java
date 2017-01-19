import java.util.logging.*;

/**
 * Created by David on 17/01/2017.
 */
public class Logging {
    public static Logger logger;
    public static FileHandler handler;



    public static void init(String LOGFILE_PREFIX, String BOT_NAME, String LOGFILE_SUFFIX, Level LOGGING_LEVEL) {
        try {
            logger = Logger.getLogger(BOT_NAME);
            logger.setUseParentHandlers(false);
            handler = new FileHandler(String.format("%s%s - %d%s",
                    LOGFILE_PREFIX, BOT_NAME,
                    System.currentTimeMillis() / 1000, LOGFILE_SUFFIX));
            logger.addHandler(handler);
            Formatter formatter = new SimpleFormatter();
            handler.setFormatter(formatter);
            logger.setLevel(LOGGING_LEVEL);
            logger.info("Link starto!");


        } catch (Exception e)

        {
            e.printStackTrace();
        }


    }
}



