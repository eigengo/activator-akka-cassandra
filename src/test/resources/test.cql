DROP TABLE tweets;

CREATE TABLE tweets (
  key text PRIMARY KEY,
  user_user varchar,
  text varchar,
  createdat timestamp
  ) WITH comment='Tweets'
    AND read_repair_chance = 1.0;