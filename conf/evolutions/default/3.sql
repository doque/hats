-- Author: Nemo

# --- !Ups
CREATE TABLE event (
  id                 	BIGINT NOT NULL PRIMARY KEY,
  type					varchar(255) NOT NULL,
  thinking_session		BIGINT NOT NULL REFERENCES thinking_session(id),
  hat                  	BIGINT NOT NULL REFERENCES hat(id),
  user					BIGINT REFERENCES user(id),
  card					BIGINT REFERENCES card(id),
  bucket				BIGINT REFERENCES bucket(id),
  time					datetime DEFAULT NOW()
);

# --- !Downs
DROP TABLE if exists event;