package org.apache.tomcat.util.buf;


import org.jboss.logging.*;

import static org.jboss.logging.Logger.Level.WARN;

@MessageLogger(projectCode = "JBWEB")
public interface BufLogger extends BasicLogger {

    /**
     * A logger with the category of the package name.
     */
    BufLogger ROOT_LOGGER = Logger.getMessageLogger(BufLogger.class, "org.apache.tomcat.util.buf");

    @LogMessage(level = WARN)
    @Message(id = 9500, value = "Failed to reset instance of decoder for character set [{0}]")
    void decoderResetFail();

    @LogMessage(level = WARN)
    @Message(id = 9501, value = "Failed to reset instance of encoder for character set [{0}]")
    void encoderResetFail();

}
