package com.reactivebbq.orders;

import org.h2.tools.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;

import static org.h2.tools.Server.*;

public class H2Database {
    public static void main(String[] args) {
        Logger logger = LoggerFactory.getLogger("root");

        logger.info("STARTING H2 DATABASE SERVER");

        try {
            Server server = createTcpServer("-ifNotExists");

            server.start();

            logger.info("H2 DATABASE SERVER IS RUNNING");
        } catch (SQLException ex) {
            logger.error("FAILED TO INITIALIZE H2 DATABASE.", ex);
        }
    }
}
