--GP-557
CREATE TABLE avvisi
(
       cod_dominio VARCHAR(35) NOT NULL,
       iuv VARCHAR(35) NOT NULL,
       -- Precisione ai millisecondi supportata dalla versione 5.6.4, se si utilizza una versione precedente non usare il suffisso '(3)'
       data_creazione TIMESTAMP(3) NOT NULL DEFAULT 0,
       stato VARCHAR(255) NOT NULL,
       pdf MEDIUMBLOB,
       -- fk/pk columns
       id BIGINT AUTO_INCREMENT,
       -- check constraints
       -- fk/pk keys constraints
       CONSTRAINT pk_avvisi PRIMARY KEY (id)
)ENGINE INNODB CHARACTER SET latin1 COLLATE latin1_general_cs;

-- index
CREATE INDEX index_avvisi_1 ON avvisi (cod_dominio,iuv);

insert into sonde(nome, classe, soglia_warn, soglia_error) values ('generazione-avvisi', 'org.openspcoop2.utils.sonde.impl.SondaBatch', 3600000, 21600000);

ALTER TABLE domini ADD COLUMN cbill VARCHAR(255);
ALTER TABLE uo ADD COLUMN uo_area VARCHAR(255);
ALTER TABLE uo ADD COLUMN uo_url_sito_web VARCHAR(255);
ALTER TABLE uo ADD COLUMN uo_email VARCHAR(255);
ALTER TABLE uo ADD COLUMN uo_pec VARCHAR(255);

alter table incassi add COLUMN id_operatore BIGINT;
alter table incassi add CONSTRAINT fk_inc_id_operatore FOREIGN KEY (id_operatore) REFERENCES operatori(id); 


CREATE TABLE pagamenti_portale
(
       cod_portale VARCHAR(35) NOT NULL,
       cod_canale VARCHAR(35),
       nome VARCHAR(255) NOT NULL,
       importo DOUBLE NOT NULL,
       versante_identificativo VARCHAR(35),
       id_sessione VARCHAR(35) NOT NULL,
       id_sessione_portale VARCHAR(35),
       id_sessione_psp VARCHAR(35),
       stato VARCHAR(35) NOT NULL,
       codice_stato VARCHAR(35) NOT NULL,
       descrizione_stato VARCHAR(1024),
       psp_redirect_url VARCHAR(1024),
       psp_esito VARCHAR(255),
       json_request LONGTEXT,
       wisp_id_dominio VARCHAR(255),
       wisp_key_pa VARCHAR(255),
       wisp_key_wisp VARCHAR(255),
       wisp_html LONGTEXT,
       -- Precisione ai millisecondi supportata dalla versione 5.6.4, se si utilizza una versione precedente non usare il suffisso '(3)'
       data_richiesta TIMESTAMP(3) DEFAULT 0,
       url_ritorno VARCHAR(1024) NOT NULL,
       cod_psp VARCHAR(35),
       tipo_versamento VARCHAR(4),
       -- fk/pk columns
       id BIGINT AUTO_INCREMENT,
       -- unique constraints
       CONSTRAINT unique_pagamenti_portale_1 UNIQUE (id_sessione),
       -- fk/pk keys constraints
       CONSTRAINT pk_pagamenti_portale PRIMARY KEY (id)
)ENGINE INNODB CHARACTER SET latin1 COLLATE latin1_general_cs;

-- index
CREATE UNIQUE INDEX index_pagamenti_portale_1 ON pagamenti_portale (id_sessione);



CREATE TABLE pag_port_versamenti
(
       -- fk/pk columns
       id BIGINT AUTO_INCREMENT,
       id_pagamento_portale BIGINT NOT NULL,
       id_versamento BIGINT NOT NULL,
       -- fk/pk keys constraints
       CONSTRAINT fk_ppv_id_pagamento_portale FOREIGN KEY (id_pagamento_portale) REFERENCES pagamenti_portale(id),
       CONSTRAINT fk_ppv_id_versamento FOREIGN KEY (id_versamento) REFERENCES versamenti(id),
       CONSTRAINT pk_pag_port_versamenti PRIMARY KEY (id)
)ENGINE INNODB CHARACTER SET latin1 COLLATE latin1_general_cs;

ALTER TABLE rpt ADD COLUMN id_pagamento_portale BIGINT;
ALTER TABLE rpt ADD CONSTRAINT fk_rpt_id_pagamento_portale FOREIGN KEY (id_pagamento_portale) REFERENCES pagamenti_portale(id);

ALTER TABLE versamenti ADD COLUMN data_validita TIMESTAMP(3);
ALTER TABLE versamenti ADD COLUMN nome VARCHAR(35);
--TODO not null
ALTER TABLE versamenti ADD COLUMN tassonomia_avviso VARCHAR(35);
--TODO not null
ALTER TABLE versamenti ADD COLUMN tassonomia VARCHAR(35);
ALTER TABLE versamenti ADD COLUMN id_dominio BIGINT;
update versamenti set id_dominio = (select id_dominio from uo where id = versamenti.id_uo);
ALTER TABLE versamenti MODIFY COLUMN id_dominio BIGINT NOT NULL;
ALTER TABLE versamenti ADD COLUMN debitore_tipo VARCHAR(1);


ALTER TABLE acl DROP COLUMN id_portale;

ALTER TABLE rpt DROP COLUMN id_portale;

ALTER TABLE rpt ADD COLUMN id_applicazione BIGINT;
ALTER TABLE rpt ADD CONSTRAINT fk_rpt_id_applicazione FOREIGN KEY (id_applicazione) REFERENCES applicazioni(id);

DROP TABLE portali;
DROP SEQUENCE portali_seq;

ALTER TABLE applicazioni ADD COLUMN reg_exp VARCHAR(1024);

ALTER TABLE domini DROP COLUMN xml_conti_accredito;
ALTER TABLE domini DROP COLUMN xml_tabella_controparti;



DROP TABLE acl;
CREATE TABLE acl
(
	ruolo VARCHAR(255),
	principal VARCHAR(255),
	servizio VARCHAR(255) NOT NULL,
	VARCHAR(255) NOT NULL,
	-- fk/pk columns
	id BIGINT AUTO_INCREMENT,
	-- fk/pk keys constraints
	CONSTRAINT pk_acl PRIMARY KEY (id)
)ENGINE INNODB CHARACTER SET latin1 COLLATE latin1_general_cs;


DROP TABLE ruoli;


CREATE TABLE utenze
(
	principal VARCHAR(255) NOT NULL,
        abilitato BOOLEAN NOT NULL DEFAULT true,
	-- fk/pk columns
	id BIGINT AUTO_INCREMENT,
	-- unique constraints
	CONSTRAINT unique_utenze_1 UNIQUE (principal),
	-- fk/pk keys constraints
	CONSTRAINT pk_utenze PRIMARY KEY (id)
)ENGINE INNODB CHARACTER SET latin1 COLLATE latin1_general_cs;

-- index
CREATE INDEX index_utenze_1 ON utenze (principal);


CREATE TABLE utenze_domini
(
	-- fk/pk columns
	id BIGINT AUTO_INCREMENT,
	id_utenza BIGINT NOT NULL,
	id_dominio BIGINT NOT NULL,
	-- fk/pk keys constraints
	CONSTRAINT fk_nzd_id_utenza FOREIGN KEY (id_utenza) REFERENCES utenze(id),
	CONSTRAINT fk_nzd_id_dominio FOREIGN KEY (id_dominio) REFERENCES domini(id),
	CONSTRAINT pk_utenze_domini PRIMARY KEY (id)
)ENGINE INNODB CHARACTER SET latin1 COLLATE latin1_general_cs;




CREATE TABLE utenze_tributi
(
	-- fk/pk columns
	id BIGINT AUTO_INCREMENT,
	id_utenza BIGINT NOT NULL,
	id_tributo BIGINT NOT NULL,
	-- fk/pk keys constraints
	CONSTRAINT fk_nzt_id_utenza FOREIGN KEY (id_utenza) REFERENCES utenze(id),
	CONSTRAINT fk_nzt_id_tributo FOREIGN KEY (id_tributo) REFERENCES tributi(id),
	CONSTRAINT pk_utenze_tributi PRIMARY KEY (id)
)ENGINE INNODB CHARACTER SET latin1 COLLATE latin1_general_cs;



insert into utenze (principal) select distinct principal from ((select principal from operatori) union (select principal from applicazioni) )as s;


ALTER TABLE applicazioni ADD COLUMN id_utenza BIGINT;
UPDATE applicazioni set id_utenza = (select id from utenze where principal = applicazioni.principal);
ALTER TABLE applicazioni MODIFY COLUMN id_utenza BIGINT NOT NULL;
ALTER TABLE applicazioni DROP COLUMN principal;
ALTER TABLE applicazioni DROP COLUMN abilitato;


ALTER TABLE operatori ADD COLUMN id_utenza BIGINT;
UPDATE operatori set id_utenza = (select id from utenze where principal = operatori.principal);
ALTER TABLE operatori MODIFY COLUMN id_utenza BIGINT NOT NULL;
ALTER TABLE operatori DROP COLUMN principal;
ALTER TABLE operatori DROP COLUMN profilo;
ALTER TABLE operatori DROP COLUMN abilitato;

ALTER TABLE applicazioni ADD COLUMN auto_iuv BOOLEAN;
UPDATE applicazioni SET auto_iuv = true;
ALTER TABLE applicazioni MODIFY COLUMN auto_iuv BOOLEAN NOT NULL;

ALTER TABLE domini DROP COLUMN custom_iuv;
ALTER TABLE domini DROP COLUMN iuv_prefix_strict;
ALTER TABLE domini DROP COLUMN riuso_iuv;

ALTER TABLE iban_accredito DROP COLUMN id_seller_bank;
ALTER TABLE iban_accredito DROP COLUMN id_negozio;
ALTER TABLE applicazioni DROP COLUMN versione;

ALTER TABLE versamenti add column dati_allegati LONGTEXT;
ALTER TABLE singoli_versamenti add column dati_allegati LONGTEXT;
ALTER TABLE singoli_versamenti add column descrizione VARCHAR(256);
ALTER TABLE singoli_versamenti drop column note;

ALTER TABLE iban_accredito DROP COLUMN iban_appoggio;
ALTER TABLE iban_accredito DROP COLUMN bic_appoggio;

ALTER TABLE tributi RENAME COLUMN id_iban_accredito_postale TO id_iban_appoggio;

ALTER TABLE singoli_versamenti add column id_iban_appoggio BIGINT;
ALTER TABLE singoli_versamenti add CONSTRAINT fk_sng_id_iban_appoggio FOREIGN KEY (id_iban_appoggio) REFERENCES iban_accredito(id);

ALTER TABLE incassi add column iban_accredito VARCHAR(35);
ALTER TABLE incassi ADD CONSTRAINT unique_incassi_1 UNIQUE (cod_dominio,trn);

ALTER TABLE pagamenti ADD COLUMN tipo VARCHAR(35);
update pagamenti set tipo = 'ENTRATA' where iban_accredito is not null;
update pagamenti set tipo = 'MBT' where iban_accredito is null;
ALTER TABLE pagamenti DROP COLUMN iban_accredito;
ALTER TABLE pagamenti MODIFY COLUMN tipo VARCHAR(35) NOT NULL;

ALTER TABLE versamenti ADD COLUMN incasso VARCHAR(1);
ALTER TABLE versamenti ADD COLUMN anomalie LONGTEXT;

ALTER TABLE versamenti ADD COLUMN iuv_versamento VARCHAR(35);
ALTER TABLE versamenti ADD COLUMN numero_avviso VARCHAR(35);
ALTER TABLE versamenti ADD COLUMN avvisatura VARCHAR(1);
ALTER TABLE versamenti ADD COLUMN tipo_pagamento INT;

ALTER TABLE utenze MODIFY COLUMN principal VARCHAR(4000);
ALTER TABLE utenze ADD COLUMN principal_originale VARCHAR(4000);
update utenze set principal_originale = principal;
ALTER TABLE utenze MODIFY COLUMN principal_originale VARCHAR(4000) NOT NULL;


