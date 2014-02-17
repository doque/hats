-- Author: Nemo

# --- !Ups
CREATE SEQUENCE user_id_seq;
-- needs \"\" because user is a reserved SQL table. To keep the Scala Play convention we keep the singular as name
CREATE TABLE `user` (
  id                    BIGINT NOT NULL DEFAULT nextval('user_id_seq') PRIMARY KEY,
  name                  varchar(255) NOT NULL,
  mail                  varchar(255) DEFAULT NULL
);

CREATE TABLE hat (
  id                    BIGINT NOT NULL PRIMARY KEY,
  name                  varchar(255) NOT NULL
);

CREATE SEQUENCE thinking_session_id_seq;
CREATE TABLE thinking_session (
  id                    BIGINT NOT NULL DEFAULT nextval('thinking_session_id_seq') PRIMARY KEY,
  owner                 BIGINT NOT NULL REFERENCES `user`(id),
  title                 varchar(255) NOT NULL,
  current_hat           integer REFERENCES hat(id),
  finished              BIT DEFAULT False
);


CREATE TABLE bucket (
  id                  BIGINT NOT NULL PRIMARY KEY,
  thinking_session    BIGINT NOT NULL REFERENCES thinking_session(id),
  name               text DEFAULT ''
);



CREATE TABLE card (
  id                    BIGINT NOT NULL DEFAULT nextval('card_id_seq') PRIMARY KEY,
  thinking_session      BIGINT NOT NULL REFERENCES thinking_session(id),
  bucket                BIGINT REFERENCES bucket(id),
  content               text NOT NULL,
  hat						        BIGINT REFERENCES hat(id),
  creator					      BIGINT REFERENCES `user`(id),
  time                  datetime DEFAULT NOW()
);

  
-- preload hat table with the 6 hats
INSERT INTO hat VALUES (1,'White');
INSERT INTO hat VALUES (2,'Red');
INSERT INTO hat VALUES (3,'Yellow');
INSERT INTO hat VALUES (4,'Black');
INSERT INTO hat VALUES (5,'Green');
INSERT INTO hat VALUES (6,'Blue');

-- preload user table with the developer dummies
INSERT INTO user VALUES (1,'David',NULL);
INSERT INTO user VALUES (2,'Manu',NULL);
INSERT INTO user VALUES (3,'Dom',NULL);
INSERT INTO user VALUES (4,'Anamika',NULL);

INSERT INTO thinking_session VALUES (0, 1, 'Birthday Present for Manu', 1, False);

# --- !Downs
DROP TABLE if exists hat;

DROP TABLE if exists `user`;
DROP SEQUENCE if EXISTS user_id_seq;

DROP TABLE if exists thinking_session;
DROP SEQUENCE if EXISTS thinking_session_id_seq;

DROP TABLE if exists card;

DROP TABLE if exists bucket;