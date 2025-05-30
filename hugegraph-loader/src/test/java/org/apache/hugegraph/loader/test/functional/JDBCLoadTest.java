/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.apache.hugegraph.loader.test.functional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.hugegraph.loader.HugeGraphLoader;
import org.apache.hugegraph.structure.graph.Edge;
import org.apache.hugegraph.structure.graph.Vertex;
import org.apache.hugegraph.testutil.Assert;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * TODO: add more test cases
 */
public class JDBCLoadTest extends LoadTest {

    // JDBC driver name and database URL
    private static final String DRIVER = "com.mysql.cj.jdbc.Driver";
    private static final String DATABASE = "load_test";
    private static final String DB_URL = "jdbc:mysql://127.0.0.1:3306";
    // Database credentials
    private static final String USER = "root";
    private static final String PASS = "root";

    private static final DBUtil dbUtil = new DBUtil(DRIVER, DB_URL, USER, PASS);
    private static final boolean USE_SSL = false;

    @BeforeClass
    public static void setUp() {
        clearServerData();

        dbUtil.connect(USE_SSL);
        // create database
        dbUtil.execute(String.format("CREATE DATABASE IF NOT EXISTS `%s`;", DATABASE));
        // create tables
        dbUtil.connect(DATABASE, USE_SSL);
        // vertex person
        dbUtil.execute("CREATE TABLE IF NOT EXISTS `person` (" +
                       "`id` int(10) unsigned NOT NULL," +
                       "`name` varchar(20) NOT NULL," +
                       "`age` int(3) DEFAULT NULL," +
                       "`city` varchar(10) DEFAULT NULL," +
                       "PRIMARY KEY (`id`)" +
                       ") ENGINE=InnoDB DEFAULT CHARSET=utf8;");
        // vertex software
        dbUtil.execute("CREATE TABLE IF NOT EXISTS `software` (" +
                       "`id` int(10) unsigned NOT NULL," +
                       "`name` varchar(20) NOT NULL," +
                       "`lang` varchar(10) NOT NULL," +
                       "`price` double(10,2) NOT NULL," +
                       "PRIMARY KEY (`id`)" +
                       ") ENGINE=InnoDB DEFAULT CHARSET=utf8;");
        // vertex date
        dbUtil.execute("CREATE TABLE IF NOT EXISTS `date_test` (" +
                       "`id` int(10) unsigned NOT NULL," +
                       "`calendar_date` DATE NOT NULL," +
                       "`calendar_datetime` DATETIME NOT NULL," +
                       "`calendar_timestamp` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                       "`calendar_time` TIME NOT NULL," +
                       "`calendar_year` YEAR NOT NULL," +
                       "PRIMARY KEY (`id`)" +
                       ") ENGINE=InnoDB DEFAULT CHARSET=utf8;");

        // edge knows
        dbUtil.execute("CREATE TABLE IF NOT EXISTS `knows` (" +
                       "`id` int(10) unsigned NOT NULL," +
                       "`source_id` int(10) unsigned NOT NULL," +
                       "`target_id` int(10) unsigned NOT NULL," +
                       "`date` varchar(10) NOT NULL," +
                       "`weight` double(10,2) NOT NULL," +
                       "PRIMARY KEY (`id`)" +
                       ") ENGINE=InnoDB DEFAULT CHARSET=utf8;");
        // edge created
        dbUtil.execute("CREATE TABLE IF NOT EXISTS `created` (" +
                       "`id` int(10) unsigned NOT NULL," +
                       "`source_id` int(10) unsigned NOT NULL," +
                       "`target_id` int(10) unsigned NOT NULL," +
                       "`date` varchar(10) NOT NULL," +
                       "`weight` double(10,2) NOT NULL," +
                       "PRIMARY KEY (`id`)" +
                       ") ENGINE=InnoDB DEFAULT CHARSET=utf8;");

    }

    @AfterClass
    public static void tearDown() {
        // drop tables
        dbUtil.execute("DROP TABLE IF EXISTS `person`");
        dbUtil.execute("DROP TABLE IF EXISTS `software`");
        dbUtil.execute("DROP TABLE IF EXISTS `knows`");
        dbUtil.execute("DROP TABLE IF EXISTS `created`");
        dbUtil.execute("DROP TABLE IF EXISTS `date_test`");
        // drop database
        dbUtil.execute(String.format("DROP DATABASE `%s`", DATABASE));

        dbUtil.close();
    }

    @Before
    public void init() {
    }

    @After
    public void clear() {
        clearServerData();

        dbUtil.execute("TRUNCATE TABLE `person`");
        dbUtil.execute("TRUNCATE TABLE `software`");
        dbUtil.execute("TRUNCATE TABLE `knows`");
        dbUtil.execute("TRUNCATE TABLE `created`");
        dbUtil.execute("TRUNCATE TABLE `date_test`");
    }

    @Test
    public void testCustomizedSchema() {
        dbUtil.insert("INSERT INTO `person` VALUES " +
                      "(1,'marko',29,'Beijing')," +
                      "(2,'vadas',27,'HongKong')," +
                      "(3,'josh',32,'Beijing')," +
                      "(4,'peter',35,'Shanghai')," +
                      "(5,'li,nary',26,'Wu,han')," +
                      "(6,'tom',NULL,NULL);");
        dbUtil.insert("INSERT INTO `software` VALUES " +
                      "(100,'lop','java',328.00)," +
                      "(200,'ripple','java',199.00);");

        dbUtil.insert("INSERT INTO `knows` VALUES " +
                      "(1,1,2,'2016-01-10',0.50)," +
                      "(2,1,3,'2013-02-20',1.00);");
        dbUtil.insert("INSERT INTO `created` VALUES " +
                      "(1,1,100,'2017-12-10',0.40)," +
                      "(2,3,100,'2009-11-11',0.40)," +
                      "(3,3,200,'2017-12-10',1.00)," +
                      "(4,4,100,'2017-03-24',0.20);");

        String[] args = new String[]{
                "-f", configPath("jdbc_customized_schema/struct.json"),
                "-s", configPath("jdbc_customized_schema/schema.groovy"),
                "-g", GRAPH,
                "-h", SERVER,
                "-p", String.valueOf(PORT),
                "--batch-insert-threads", "2",
                "--test-mode", "true"
        };
        HugeGraphLoader.main(args);

        List<Vertex> vertices = CLIENT.graph().listVertices();
        List<Edge> edges = CLIENT.graph().listEdges();

        Assert.assertEquals(8, vertices.size());
        Assert.assertEquals(6, edges.size());

        for (Vertex vertex : vertices) {
            Assert.assertEquals(Integer.class, vertex.id().getClass());
        }
        for (Edge edge : edges) {
            Assert.assertEquals(Integer.class, edge.sourceId().getClass());
            Assert.assertEquals(Integer.class, edge.targetId().getClass());
        }
    }

    @Test
    public void testEmptyTable() {
        String[] args = new String[]{
                "-f", configPath("jdbc_customized_schema/struct.json"),
                "-s", configPath("jdbc_customized_schema/schema.groovy"),
                "-g", GRAPH,
                "-h", SERVER,
                "-p", String.valueOf(PORT),
                "--batch-insert-threads", "2",
                "--test-mode", "true"
        };
        HugeGraphLoader.main(args);

        List<Vertex> vertices = CLIENT.graph().listVertices();
        List<Edge> edges = CLIENT.graph().listEdges();

        Assert.assertEquals(0, vertices.size());
        Assert.assertEquals(0, edges.size());
    }

    @Test
    public void testValueMappingInJDBCSource() {
        dbUtil.insert("INSERT INTO `person` VALUES " +
                      "(1,'marko',29,'1')," +
                      "(2,'vadas',27,'2');");

        String[] args = new String[]{
                "-f", configPath("jdbc_value_mapping/struct.json"),
                "-s", configPath("jdbc_value_mapping/schema.groovy"),
                "-g", GRAPH,
                "-h", SERVER,
                "-p", String.valueOf(PORT),
                "--batch-insert-threads", "2",
                "--test-mode", "true"
        };
        HugeGraphLoader.main(args);

        List<Vertex> vertices = CLIENT.graph().listVertices();
        Assert.assertEquals(2, vertices.size());
        assertContains(vertices, "person", "name", "marko",
                       "age", 29, "city", "Beijing");
        assertContains(vertices, "person", "name", "vadas",
                       "age", 27, "city", "Shanghai");
    }

    @Test
    public void testNumberToStringInJDBCSource() {
        dbUtil.insert("INSERT INTO `person` VALUES " +
                      "(1,'marko',29,'Beijing')," +
                      "(2,'vadas',27,'HongKong')," +
                      "(3,'josh',32,'Beijing')," +
                      "(4,'peter',35,'Shanghai')," +
                      "(5,'li,nary',26,'Wu,han')," +
                      "(6,'tom',NULL,NULL);");

        dbUtil.insert("INSERT INTO `software` VALUES " +
                      "(100,'lop','java',328.08)," +
                      "(200,'ripple','java',199.67);");

        String[] args = new String[]{
                "-f", configPath("jdbc_number_to_string/struct.json"),
                "-s", configPath("jdbc_number_to_string/schema.groovy"),
                "-g", GRAPH,
                "-h", SERVER,
                "-p", String.valueOf(PORT),
                "--batch-insert-threads", "2",
                "--test-mode", "true"
        };
        HugeGraphLoader.main(args);

        List<Vertex> vertices = CLIENT.graph().listVertices();

        Assert.assertEquals(8, vertices.size());
        assertContains(vertices, "person", "age", "29");
        assertContains(vertices, "software", "price", "199.67");
    }

    @Test
    public void testJdbcSqlDateConvert() {
        dbUtil.execute("INSERT INTO `date_test` VALUES " +
                       "(1, '2017-12-10', '2017-12-10 15:30:45', '2017-12-10 15:30:45', " +
                       "'15:30:45', '2017')," +
                       "(2, '2009-11-11', '2009-11-11 08:15:30', '2009-11-11 08:15:30', " +
                       "'08:15:30', '2009')," +
                       "(3, '2017-03-24', '2017-03-24 12:00:00', '2017-03-24 12:00:00', " +
                       "'12:00:00', '2017');");

        String[] args = new String[]{
                "-f", configPath("jdbc_sql_date_convert/struct.json"),
                "-s", configPath("jdbc_sql_date_convert/schema.groovy"),
                "-g", GRAPH,
                "-h", SERVER,
                "-p", String.valueOf(PORT),
                "--batch-insert-threads", "2",
                "--test-mode", "true"
        };
        HugeGraphLoader.main(args);

        List<Vertex> vertices = CLIENT.graph().listVertices();

        Assert.assertEquals(3, vertices.size());
        // Define formatters
        DateTimeFormatter serverDateFormatter =
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

        // DATE check
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDateTime date = LocalDate.parse("2017-12-10", dateFormatter).atStartOfDay();

        DateTimeFormatter datetimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime datetime = LocalDateTime.parse("2017-12-10 15:30:45", datetimeFormatter);

        DateTimeFormatter timestampFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime timestamp = LocalDateTime.parse("2017-12-10 15:30:45", timestampFormatter);

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        LocalTime time = LocalTime.parse("15:30:45", timeFormatter);
        // Supplement the date as the Epoch start
        LocalDateTime timeWithDate = time.atDate(LocalDate.of(1970, 1, 1));

        DateTimeFormatter yearFormatter = DateTimeFormatter.ofPattern("yyyy");
        Year year = Year.parse("2017", yearFormatter);
        // Supplement the date as the first day of the year
        LocalDateTime yearStart = year.atDay(1).atStartOfDay();

        assertContains(vertices, "date_test",
                       "calendar_date", date.format(serverDateFormatter),
                       "calendar_datetime", datetime.format(serverDateFormatter),
                       "calendar_timestamp", timestamp.format(serverDateFormatter),
                       "calendar_time", timeWithDate.format(serverDateFormatter),
                       "calendar_year", yearStart.format(serverDateFormatter));
    }
}
