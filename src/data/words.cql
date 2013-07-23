DROP TABLE positivewords;

CREATE TABLE positivewords (
  key int PRIMARY KEY,
  word text
) WITH comment='Good sentiment words'
  AND COMPACT STORAGE
  AND read_repair_chance = 1.0;

DROP TABLE negativewords;

CREATE TABLE negativewords (
  key int PRIMARY KEY,
  word text
) WITH comment='Bad sentiment words'
  AND COMPACT STORAGE
  AND read_repair_chance = 1.0;

COPY positivewords (key, word) FROM 'positive_words.csv';
COPY negativewords (key, word) FROM 'negative_words.csv';
