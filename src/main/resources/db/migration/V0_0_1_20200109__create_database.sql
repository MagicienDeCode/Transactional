create table IF NOT EXISTS woman
(
    id         BINARY(16),
    created_at timestamp DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp ON UPDATE CURRENT_TIMESTAMP,
    reference  varchar(255),
    primary key (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8;
create table IF NOT EXISTS man
(
    id         BINARY(16),
    created_at timestamp DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp ON UPDATE CURRENT_TIMESTAMP,
    reference  varchar(255),
    woman_id BINARY(16),
    primary key (id),
    constraint FK_MAN_WOMAN foreign key (woman_id) references woman(id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8;