DROP TABLE tweets;

CREATE TABLE tweets (
  key text PRIMARY KEY,
  user_user varchar,
  text text,
  createdat timestamp
  ) WITH comment='Tweets'
    AND COMPACT STORAGE
    AND read_repair_chance = 1.0;