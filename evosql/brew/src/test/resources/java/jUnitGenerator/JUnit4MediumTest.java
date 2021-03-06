package brew.test.generated;

import java.lang.String;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import javax.annotation.Generated;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

@Generated("nl.tudelft.serg.evosql.brew.generator.junit.JUnit4TestGenerator")
public class JUnit4MediumTest {
  /**
   * The production query used to test the generated fixtures on.
   */
  private static final String PRODUCTION_QUERY = "Select * From table1, products Where column1_2 < 1 And expired = 0;";

  /**
   * This method should connect to your database and execute the given query.
   * In order for the assertions to work correctly this method must return a list of maps
   * in the case that the query succeeds, or null if the query fails. The tests will assert the results.
   *
   * @param query    The query to execute.
   * @param isUpdate Whether the query is a data modification statement.
   *
   * @returns The resulting table, or null if the query is an update.
   */
  private static ArrayList<HashMap<String, String>> runSql(String query, boolean isUpdate) throws
      SQLException {
    // TODO: implement method stub.
    return null;
  }

  /**
   * Generates a string map from a list of strings.
   */
  private static HashMap<String, String> makeMap(String... strings) {
    HashMap<String, String> result = new HashMap<>();
    for(int i = 0; i < strings.length; i += 2) {
      result.put(strings[i], strings[i + 1]);
    }
    return result;
  }

  /**
   * Creates tables required for queries.
   */
  private static void createTables() throws SQLException {
    runSql("CREATE TABLE `products` (`product_name` VARCHAR(100), `expired` BIT, `expiry_date` DATETIME);", true);
    runSql("CREATE TABLE `table1` (`column1_1` INTEGER, `column1_2` VARCHAR(100));", true);
  }

  /**
   * Truncates the tables.
   */
  private static void cleanTables() throws SQLException {
    runSql("TRUNCATE TABLE `products`;", true);
    runSql("TRUNCATE TABLE `table1`;", true);
  }

  /**
   * Drops the tables.
   */
  private static void dropTables() throws SQLException {
    runSql("DROP TABLE `products`;", true);
    runSql("DROP TABLE `table1`;", true);
  }

  @BeforeClass
  public static void beforeAll() throws SQLException {
  }

  @Before
  public void beforeEach() throws SQLException {
  }

  @After
  public void afterEach() throws SQLException {
    cleanTables();
  }

  @AfterClass
  public static void afterAll() throws SQLException {
    dropTables();
  }

  @Test
  public void generatedTest1() throws SQLException {
    // Arrange: set up the fixture data
    runSql("INSERT INTO `table1` (`column1_1`, `column1_2`) VALUES (1, 'String of row 1'), (2, 'String of row 2');", true);
    runSql("INSERT INTO `products` (`product_name`, `expired`, `expiry_date`) VALUES ('Milk', 0, '2018-03-22 00:00:00'), ('Yogurt', 1, '2018-03-15 00:00:00'), ('Salt', 0, '2025-12-31 23:59:59');", true);
    // Act: run a selection query on the database
    ArrayList<HashMap<String, String>> result = runSql(PRODUCTION_QUERY, false);
    // Assert: verify that the expected number of rows is returned
    Assert.assertEquals(1, result.size());
    // Assert: verify that the results are correct
    Assert.assertTrue(result.contains(makeMap()));
  }

  @Test
  public void generatedTest2() throws SQLException {
    // Arrange: set up the fixture data
    runSql("INSERT INTO `table1` (`column1_1`, `column1_2`) VALUES (1, 'String of row 1'), (2, 'String of row 2');", true);
    runSql("INSERT INTO `products` (`product_name`, `expired`, `expiry_date`) VALUES ('Milk', 0, '2018-03-22 00:00:00'), ('Yogurt', 1, '2018-03-15 00:00:00'), ('Salt', 0, '2025-12-31 23:59:59');", true);
    // Act: run a selection query on the database
    ArrayList<HashMap<String, String>> result = runSql(PRODUCTION_QUERY, false);
    // Assert: verify that the expected number of rows is returned
    Assert.assertEquals(0, result.size());
  }

  @Test
  public void generatedTest3() throws SQLException {
    // Arrange: set up the fixture data
    runSql("INSERT INTO `table1` (`column1_1`, `column1_2`) VALUES (1, 'String of row 1'), (2, 'String of row 2');", true);
    runSql("INSERT INTO `products` (`product_name`, `expired`, `expiry_date`) VALUES ('Milk', 0, '2018-03-22 00:00:00'), ('Yogurt', 1, '2018-03-15 00:00:00'), ('Salt', 0, '2025-12-31 23:59:59');", true);
    // Act: run a selection query on the database
    ArrayList<HashMap<String, String>> result = runSql(PRODUCTION_QUERY, false);
    // Assert: verify that the expected number of rows is returned
    Assert.assertEquals(0, result.size());
  }

  @Test
  public void generatedTest4() throws SQLException {
    // Arrange: set up the fixture data
    runSql("INSERT INTO `table1` (`column1_1`, `column1_2`) VALUES (1, 'String of row 1'), (2, 'String of row 2');", true);
    runSql("INSERT INTO `products` (`product_name`, `expired`, `expiry_date`) VALUES ('Milk', 0, '2018-03-22 00:00:00'), ('Yogurt', 1, '2018-03-15 00:00:00'), ('Salt', 0, '2025-12-31 23:59:59');", true);
    // Act: run a selection query on the database
    ArrayList<HashMap<String, String>> result = runSql(PRODUCTION_QUERY, false);
    // Assert: verify that the expected number of rows is returned
    Assert.assertEquals(1, result.size());
    // Assert: verify that the results are correct
    Assert.assertTrue(result.contains(makeMap()));
  }
}
