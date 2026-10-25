"""
LeetCode SQL 50 - The Select Essentials (5 Problems)
Engine: MySQL 8.0 exclusively.
Standard LeetCode problem descriptions, schema definitions, examples,
realistic dbInit tables, and calibrated constraints.
"""

CODING_PROBLEMS_SQL = [
    # --------------------------------------------------------------------------
    # 10. Recyclable and Low Fat Products (LeetCode 1757)
    # --------------------------------------------------------------------------
    {
        "title": "Recyclable and Low Fat Products",
        "slug": "recyclable-and-low-fat-products",
        "difficulty": "EASY",
        "problemType": "DATABASE",
        "topics": ["Database"],
        "hints": [
            "Filter rows using WHERE low_fats = 'Y' AND recyclable = 'Y'."
        ],
        "statement": (
            "Table: `Products`\n\n"
            "```\n"
            "+-------------+---------+\n"
            "| Column Name | Type    |\n"
            "+-------------+---------+\n"
            "| product_id  | int     |\n"
            "| low_fats    | enum    |\n"
            "| recyclable  | enum    |\n"
            "+-------------+---------+\n"
            "```\n"
            "- `product_id` is the primary key (column with unique values) for this table.\n"
            "- `low_fats` is an ENUM (category) of type ('Y', 'N') where 'Y' means this product is low fat and 'N' means it is not.\n"
            "- `recyclable` is an ENUM (category) of types ('Y', 'N') where 'Y' means this product is recyclable and 'N' means it is not.\n\n"
            "Write a solution to find the ids of products that are both low fat and recyclable.\n\n"
            "Return the result table in **any order**.\n\n"
            "### Example 1:\n\n"
            "**Input:**\n"
            "Products table:\n"
            "```\n"
            "+-------------+----------+------------+\n"
            "| product_id  | low_fats | recyclable |\n"
            "+-------------+----------+------------+\n"
            "| 0           | Y        | N          |\n"
            "| 1           | Y        | Y          |\n"
            "| 2           | N        | Y          |\n"
            "| 3           | Y        | Y          |\n"
            "| 4           | N        | N          |\n"
            "+-------------+----------+------------+\n"
            "```\n\n"
            "**Output:**\n"
            "```\n"
            "+-------------+\n"
            "| product_id  |\n"
            "+-------------+\n"
            "| 1           |\n"
            "| 3           |\n"
            "+-------------+\n"
            "```\n"
            "**Explanation:** Only products 1 and 3 are both low fat and recyclable."
        ),
        "supportedLanguageIds": ["mysql-8.0"],
        "starterCode": {
            "mysql-8.0": "-- Write your MySQL query statement below\nSELECT product_id FROM Products WHERE low_fats = 'Y' AND recyclable = 'Y';\n"
        },
        "referenceSolutionLanguage": "mysql-8.0",
        "referenceSolution": "SELECT product_id FROM Products WHERE low_fats = 'Y' AND recyclable = 'Y';\n",
        "executionConstraints": {
            "mysql-8.0": {"timeLimitMs": 5000, "memoryLimitKb": 524288}
        },
        "sampleTestcases": [
            {
                "stdin": None,
                "dbInit": (
                    "DROP TABLE IF EXISTS Products;\n"
                    "CREATE TABLE Products (\n"
                    "    product_id INT PRIMARY KEY,\n"
                    "    low_fats ENUM('Y', 'N'),\n"
                    "    recyclable ENUM('Y', 'N')\n"
                    ");\n"
                    "INSERT INTO Products VALUES\n"
                    "    (0, 'Y', 'N'),\n"
                    "    (1, 'Y', 'Y'),\n"
                    "    (2, 'N', 'Y'),\n"
                    "    (3, 'Y', 'Y'),\n"
                    "    (4, 'N', 'N');\n"
                )
            }
        ],
        "hiddenTestcases": [
            {
                # Hidden case 1: None qualifying
                "stdin": None,
                "dbInit": (
                    "DROP TABLE IF EXISTS Products;\n"
                    "CREATE TABLE Products (\n"
                    "    product_id INT PRIMARY KEY,\n"
                    "    low_fats ENUM('Y', 'N'),\n"
                    "    recyclable ENUM('Y', 'N')\n"
                    ");\n"
                    "INSERT INTO Products VALUES\n"
                    "    (10, 'N', 'N'),\n"
                    "    (20, 'Y', 'N'),\n"
                    "    (30, 'N', 'Y');\n"
                )
            },
            {
                # Hidden case 2: All qualifying
                "stdin": None,
                "dbInit": (
                    "DROP TABLE IF EXISTS Products;\n"
                    "CREATE TABLE Products (\n"
                    "    product_id INT PRIMARY KEY,\n"
                    "    low_fats ENUM('Y', 'N'),\n"
                    "    recyclable ENUM('Y', 'N')\n"
                    ");\n"
                    "INSERT INTO Products VALUES\n"
                    "    (101, 'Y', 'Y'),\n"
                    "    (102, 'Y', 'Y');\n"
                )
            }
        ]
    },

    # --------------------------------------------------------------------------
    # 11. Find Customer Referee (LeetCode 584)
    # --------------------------------------------------------------------------
    {
        "title": "Find Customer Referee",
        "slug": "find-customer-referee",
        "difficulty": "EASY",
        "problemType": "DATABASE",
        "topics": ["Database"],
        "hints": [
            "Be careful with SQL Three-Valued Logic! In SQL, NULL != 2 evaluates to UNKNOWN, not TRUE.",
            "Use WHERE referee_id != 2 OR referee_id IS NULL (or COALESCE(referee_id, 0) != 2)."
        ],
        "statement": (
            "Table: `Customer`\n\n"
            "```\n"
            "+-------------+---------+\n"
            "| Column Name | Type    |\n"
            "+-------------+---------+\n"
            "| id          | int     |\n"
            "| name        | varchar |\n"
            "| referee_id  | int     |\n"
            "+-------------+---------+\n"
            "```\n"
            "- In SQL, `id` is the primary key column for this table.\n"
            "- Each row of this table indicates the id of a customer, their name, and the id of the customer who referred them.\n\n"
            "Find the names of the customer that are **not referred by** the customer with `id = 2`.\n\n"
            "Return the result table in **any order**.\n\n"
            "### Example 1:\n\n"
            "**Input:**\n"
            "Customer table:\n"
            "```\n"
            "+----+------+------------+\n"
            "| id | name | referee_id |\n"
            "+----+------+------------+\n"
            "| 1  | Will | null       |\n"
            "| 2  | Jane | null       |\n"
            "| 3  | Alex | 2          |\n"
            "| 4  | Bill | null       |\n"
            "| 5  | Zack | 1          |\n"
            "| 6  | Mark | 2          |\n"
            "+----+------+------------+\n"
            "```\n\n"
            "**Output:**\n"
            "```\n"
            "+------+\n"
            "| name |\n"
            "+------+\n"
            "| Will |\n"
            "| Jane |\n"
            "| Bill |\n"
            "| Zack |\n"
            "+------+\n"
            "```\n\n"
            "**Explanation:**\n"
            "- Customers with id 1, 2, 4 (referee_id is NULL) and id 5 (referee_id is 1) were not referred by customer 2."
        ),
        "supportedLanguageIds": ["mysql-8.0"],
        "starterCode": {
            "mysql-8.0": "-- Write your MySQL query statement below\nSELECT name FROM Customer WHERE referee_id != 2 OR referee_id IS NULL;\n"
        },
        "referenceSolutionLanguage": "mysql-8.0",
        "referenceSolution": "SELECT name FROM Customer WHERE referee_id != 2 OR referee_id IS NULL;\n",
        "executionConstraints": {
            "mysql-8.0": {"timeLimitMs": 5000, "memoryLimitKb": 524288}
        },
        "sampleTestcases": [
            {
                "stdin": None,
                "dbInit": (
                    "DROP TABLE IF EXISTS Customer;\n"
                    "CREATE TABLE Customer (\n"
                    "    id INT PRIMARY KEY,\n"
                    "    name VARCHAR(25),\n"
                    "    referee_id INT\n"
                    ");\n"
                    "INSERT INTO Customer VALUES\n"
                    "    (1, 'Will', NULL),\n"
                    "    (2, 'Jane', NULL),\n"
                    "    (3, 'Alex', 2),\n"
                    "    (4, 'Bill', NULL),\n"
                    "    (5, 'Zack', 1),\n"
                    "    (6, 'Mark', 2);\n"
                )
            }
        ],
        "hiddenTestcases": [
            {
                # Hidden case 1: All referee_ids are 2 -> Empty result
                "stdin": None,
                "dbInit": (
                    "DROP TABLE IF EXISTS Customer;\n"
                    "CREATE TABLE Customer (\n"
                    "    id INT PRIMARY KEY,\n"
                    "    name VARCHAR(25),\n"
                    "    referee_id INT\n"
                    ");\n"
                    "INSERT INTO Customer VALUES\n"
                    "    (1, 'Alice', 2),\n"
                    "    (2, 'Bob', 2);\n"
                )
            },
            {
                # Hidden case 2: Multiple non-2 and NULLs
                "stdin": None,
                "dbInit": (
                    "DROP TABLE IF EXISTS Customer;\n"
                    "CREATE TABLE Customer (\n"
                    "    id INT PRIMARY KEY,\n"
                    "    name VARCHAR(25),\n"
                    "    referee_id INT\n"
                    ");\n"
                    "INSERT INTO Customer VALUES\n"
                    "    (1, 'Charlie', NULL),\n"
                    "    (2, 'David', 3),\n"
                    "    (3, 'Eva', 4);\n"
                )
            }
        ]
    },

    # --------------------------------------------------------------------------
    # 12. Big Countries (LeetCode 595)
    # --------------------------------------------------------------------------
    {
        "title": "Big Countries",
        "slug": "big-countries",
        "difficulty": "EASY",
        "problemType": "DATABASE",
        "topics": ["Database"],
        "hints": [
            "Check for either condition using OR: area >= 3000000 OR population >= 25000000."
        ],
        "statement": (
            "Table: `World`\n\n"
            "```\n"
            "+-------------+---------+\n"
            "| Column Name | Type    |\n"
            "+-------------+---------+\n"
            "| name        | varchar |\n"
            "| continent   | varchar |\n"
            "| area        | int     |\n"
            "| population  | int     |\n"
            "| gdp         | bigint  |\n"
            "+-------------+---------+\n"
            "```\n"
            "- `name` is the primary key (column with unique values) for this table.\n"
            "- Each row of this table gives information about the name of a country, the continent to which it belongs, its area, the population, and its GDP value.\n\n"
            "A country is **big** if:\n"
            "1. It has an area of at least three million (i.e., `3,000,000 km²`), or\n"
            "2. It has a population of at least twenty-five million (i.e., `25,000,000`).\n\n"
            "Write a solution to find the name, population, and area of the **big countries**.\n\n"
            "Return the result table in **any order**.\n\n"
            "### Example 1:\n\n"
            "**Input:**\n"
            "World table:\n"
            "```\n"
            "+-------------+-----------+---------+------------+--------------+\n"
            "| name        | continent | area    | population | gdp          |\n"
            "+-------------+-----------+---------+------------+--------------+\n"
            "| Afghanistan | Asia      | 652230  | 25500100   | 20343000000  |\n"
            "| Albania     | Europe    | 28748   | 2831741    | 12960000000  |\n"
            "| Algeria     | Africa    | 2381741 | 37100000   | 188681000000 |\n"
            "| Andorra     | Europe    | 468     | 78115      | 3712000000   |\n"
            "| Angola      | Africa    | 1246700 | 20609294   | 100990000000 |\n"
            "+-------------+-----------+---------+------------+--------------+\n"
            "```\n\n"
            "**Output:**\n"
            "```\n"
            "+-------------+------------+---------+\n"
            "| name        | population | area    |\n"
            "+-------------+------------+---------+\n"
            "| Afghanistan | 25500100   | 652230  |\n"
            "| Algeria     | 37100000   | 2381741 |\n"
            "+-------------+------------+---------+\n"
            "```"
        ),
        "supportedLanguageIds": ["mysql-8.0"],
        "starterCode": {
            "mysql-8.0": "-- Write your MySQL query statement below\nSELECT name, population, area FROM World WHERE area >= 3000000 OR population >= 25000000;\n"
        },
        "referenceSolutionLanguage": "mysql-8.0",
        "referenceSolution": "SELECT name, population, area FROM World WHERE area >= 3000000 OR population >= 25000000;\n",
        "executionConstraints": {
            "mysql-8.0": {"timeLimitMs": 5000, "memoryLimitKb": 524288}
        },
        "sampleTestcases": [
            {
                "stdin": None,
                "dbInit": (
                    "DROP TABLE IF EXISTS World;\n"
                    "CREATE TABLE World (\n"
                    "    name VARCHAR(255) PRIMARY KEY,\n"
                    "    continent VARCHAR(255),\n"
                    "    area INT,\n"
                    "    population INT,\n"
                    "    gdp BIGINT\n"
                    ");\n"
                    "INSERT INTO World VALUES\n"
                    "    ('Afghanistan', 'Asia', 652230, 25500100, 20343000000),\n"
                    "    ('Albania', 'Europe', 28748, 2831741, 12960000000),\n"
                    "    ('Algeria', 'Africa', 2381741, 37100000, 188681000000),\n"
                    "    ('Andorra', 'Europe', 468, 78115, 3712000000),\n"
                    "    ('Angola', 'Africa', 1246700, 20609294, 100990000000);\n"
                )
            }
        ],
        "hiddenTestcases": [
            {
                # Hidden case 1: Large area but small population
                "stdin": None,
                "dbInit": (
                    "DROP TABLE IF EXISTS World;\n"
                    "CREATE TABLE World (\n"
                    "    name VARCHAR(255) PRIMARY KEY,\n"
                    "    continent VARCHAR(255),\n"
                    "    area INT,\n"
                    "    population INT,\n"
                    "    gdp BIGINT\n"
                    ");\n"
                    "INSERT INTO World VALUES\n"
                    "    ('Canada', 'North America', 9984670, 38000000, 1990000000000),\n"
                    "    ('Greenland', 'North America', 2166086, 56000, 3000000000);\n"
                )
            }
        ]
    },

    # --------------------------------------------------------------------------
    # 13. Article Views I (LeetCode 1148)
    # --------------------------------------------------------------------------
    {
        "title": "Article Views I",
        "slug": "article-views-i",
        "difficulty": "EASY",
        "problemType": "DATABASE",
        "topics": ["Database"],
        "hints": [
            "Filter where author_id = viewer_id.",
            "Use DISTINCT to avoid duplicate IDs if an author viewed their article multiple times.",
            "Remember to ORDER BY id ASC."
        ],
        "statement": (
            "Table: `Views`\n\n"
            "```\n"
            "+---------------+---------+\n"
            "| Column Name   | Type    |\n"
            "+---------------+---------+\n"
            "| article_id    | int     |\n"
            "| author_id     | int     |\n"
            "| viewer_id     | int     |\n"
            "| view_date     | date    |\n"
            "+---------------+---------+\n"
            "```\n"
            "- There is no primary key (column with unique values) for this table, the table may have duplicate rows.\n"
            "- Each row of this table indicates that some viewer viewed an article (written by some author) on some date.\n"
            "- Note that equal `author_id` and `viewer_id` indicate the same person.\n\n"
            "Write a solution to find all the authors that viewed at least one of their own articles.\n\n"
            "Return the result table sorted by `id` in **ascending order**.\n\n"
            "### Example 1:\n\n"
            "**Input:**\n"
            "Views table:\n"
            "```\n"
            "+------------+-----------+-----------+------------+\n"
            "| article_id | author_id | viewer_id | view_date  |\n"
            "+------------+-----------+-----------+------------+\n"
            "| 1          | 3         | 5         | 2019-08-01 |\n"
            "| 1          | 3         | 6         | 2019-08-02 |\n"
            "| 2          | 7         | 7         | 2019-08-01 |\n"
            "| 2          | 7         | 6         | 2019-08-02 |\n"
            "| 4          | 7         | 1         | 2019-07-22 |\n"
            "| 3          | 4         | 4         | 2019-07-21 |\n"
            "| 3          | 4         | 4         | 2019-07-21 |\n"
            "+------------+-----------+-----------+------------+\n"
            "```\n\n"
            "**Output:**\n"
            "```\n"
            "+----+\n"
            "| id |\n"
            "+----+\n"
            "| 4  |\n"
            "| 7  |\n"
            "+----+\n"
            "```"
        ),
        "supportedLanguageIds": ["mysql-8.0"],
        "starterCode": {
            "mysql-8.0": "-- Write your MySQL query statement below\nSELECT DISTINCT author_id AS id FROM Views WHERE author_id = viewer_id ORDER BY id ASC;\n"
        },
        "referenceSolutionLanguage": "mysql-8.0",
        "referenceSolution": "SELECT DISTINCT author_id AS id FROM Views WHERE author_id = viewer_id ORDER BY id ASC;\n",
        "executionConstraints": {
            "mysql-8.0": {"timeLimitMs": 5000, "memoryLimitKb": 524288}
        },
        "sampleTestcases": [
            {
                "stdin": None,
                "dbInit": (
                    "DROP TABLE IF EXISTS Views;\n"
                    "CREATE TABLE Views (\n"
                    "    article_id INT,\n"
                    "    author_id INT,\n"
                    "    viewer_id INT,\n"
                    "    view_date DATE\n"
                    ");\n"
                    "INSERT INTO Views VALUES\n"
                    "    (1, 3, 5, '2019-08-01'),\n"
                    "    (1, 3, 6, '2019-08-02'),\n"
                    "    (2, 7, 7, '2019-08-01'),\n"
                    "    (2, 7, 6, '2019-08-02'),\n"
                    "    (4, 7, 1, '2019-07-22'),\n"
                    "    (3, 4, 4, '2019-07-21'),\n"
                    "    (3, 4, 4, '2019-07-21');\n"
                )
            }
        ],
        "hiddenTestcases": [
            {
                # Hidden case 1: No self-views
                "stdin": None,
                "dbInit": (
                    "DROP TABLE IF EXISTS Views;\n"
                    "CREATE TABLE Views (\n"
                    "    article_id INT,\n"
                    "    author_id INT,\n"
                    "    viewer_id INT,\n"
                    "    view_date DATE\n"
                    ");\n"
                    "INSERT INTO Views VALUES\n"
                    "    (1, 10, 20, '2020-01-01'),\n"
                    "    (2, 20, 30, '2020-01-02');\n"
                )
            },
            {
                # Hidden case 2: Author viewed own article 5 times -> should appear only ONCE
                "stdin": None,
                "dbInit": (
                    "DROP TABLE IF EXISTS Views;\n"
                    "CREATE TABLE Views (\n"
                    "    article_id INT,\n"
                    "    author_id INT,\n"
                    "    viewer_id INT,\n"
                    "    view_date DATE\n"
                    ");\n"
                    "INSERT INTO Views VALUES\n"
                    "    (1, 5, 5, '2020-05-01'),\n"
                    "    (2, 5, 5, '2020-05-02'),\n"
                    "    (3, 5, 5, '2020-05-03'),\n"
                    "    (4, 5, 5, '2020-05-04'),\n"
                    "    (5, 5, 5, '2020-05-05'),\n"
                    "    (6, 2, 2, '2020-05-01');\n"
                )
            }
        ]
    },

    # --------------------------------------------------------------------------
    # 14. Invalid Tweets (LeetCode 1683)
    # --------------------------------------------------------------------------
    {
        "title": "Invalid Tweets",
        "slug": "invalid-tweets",
        "difficulty": "EASY",
        "problemType": "DATABASE",
        "topics": ["Database"],
        "hints": [
            "Use CHAR_LENGTH(content) > 15 to check the character count (not byte count)."
        ],
        "statement": (
            "Table: `Tweets`\n\n"
            "```\n"
            "+----------------+---------+\n"
            "| Column Name    | Type    |\n"
            "+----------------+---------+\n"
            "| tweet_id       | int     |\n"
            "| content        | varchar |\n"
            "+----------------+---------+\n"
            "```\n"
            "- `tweet_id` is the primary key (column with unique values) for this table.\n"
            "- `content` consists of characters on an American Keyboard, and no other special characters.\n"
            "- This table contains all the tweets in a social media app.\n\n"
            "Write a solution to find the IDs of the invalid tweets. A tweet is invalid if the number of characters used in the content of the tweet is **strictly greater than 15**.\n\n"
            "Return the result table in **any order**.\n\n"
            "### Example 1:\n\n"
            "**Input:**\n"
            "Tweets table:\n"
            "```\n"
            "+----------+-----------------------------------+\n"
            "| tweet_id | content                           |\n"
            "+----------+-----------------------------------+\n"
            "| 1        | Let us Code                       |\n"
            "| 2        | More than fifteen chars are here! |\n"
            "+----------+-----------------------------------+\n"
            "```\n\n"
            "**Output:**\n"
            "```\n"
            "+----------+\n"
            "| tweet_id |\n"
            "+----------+\n"
            "| 2        |\n"
            "+----------+\n"
            "```\n"
            "**Explanation:**\n"
            "- Tweet 1 has length = 11. It is a valid tweet.\n"
            "- Tweet 2 has length = 33. It is an invalid tweet."
        ),
        "supportedLanguageIds": ["mysql-8.0"],
        "starterCode": {
            "mysql-8.0": "-- Write your MySQL query statement below\nSELECT tweet_id FROM Tweets WHERE CHAR_LENGTH(content) > 15;\n"
        },
        "referenceSolutionLanguage": "mysql-8.0",
        "referenceSolution": "SELECT tweet_id FROM Tweets WHERE CHAR_LENGTH(content) > 15;\n",
        "executionConstraints": {
            "mysql-8.0": {"timeLimitMs": 5000, "memoryLimitKb": 524288}
        },
        "sampleTestcases": [
            {
                "stdin": None,
                "dbInit": (
                    "DROP TABLE IF EXISTS Tweets;\n"
                    "CREATE TABLE Tweets (\n"
                    "    tweet_id INT PRIMARY KEY,\n"
                    "    content VARCHAR(140)\n"
                    ");\n"
                    "INSERT INTO Tweets VALUES\n"
                    "    (1, 'Let us Code'),\n"
                    "    (2, 'More than fifteen chars are here!');\n"
                )
            }
        ],
        "hiddenTestcases": [
            {
                # Hidden case 1: Exactly 15 chars (should NOT be invalid)
                "stdin": None,
                "dbInit": (
                    "DROP TABLE IF EXISTS Tweets;\n"
                    "CREATE TABLE Tweets (\n"
                    "    tweet_id INT PRIMARY KEY,\n"
                    "    content VARCHAR(140)\n"
                    ");\n"
                    "INSERT INTO Tweets VALUES\n"
                    "    (1, '123456789012345'),\n"
                    "    (2, '1234567890123456');\n"
                )
            },
            {
                # Hidden case 2: Empty content
                "stdin": None,
                "dbInit": (
                    "DROP TABLE IF EXISTS Tweets;\n"
                    "CREATE TABLE Tweets (\n"
                    "    tweet_id INT PRIMARY KEY,\n"
                    "    content VARCHAR(140)\n"
                    ");\n"
                    "INSERT INTO Tweets VALUES\n"
                    "    (10, ''),\n"
                    "    (20, 'Short tweet');\n"
                )
            }
        ]
    }
]

