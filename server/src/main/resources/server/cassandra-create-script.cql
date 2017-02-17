
/////////////////////////////////////////////
//
//  VIKI
//

/////////////////////////////////////////////
// organisation

create table if not exists viki.organisation (
	id uuid, name text, primary_user uuid,
	primary key(name)
);

create table if not exists viki.organisation_by_id (
	id uuid, name text, primary key(id)
);

/////////////////////////////////////////////
// user

create table if not exists viki.user (
	id uuid, email text, first_name text, surname text, organisation_id uuid,
	salt uuid, password_sha256 text, confirmed boolean, system_user boolean,
	primary key(email)
);

create table if not exists viki.user_email_list (
	organisation_id uuid, email_list list<text>,
	primary key(organisation_id)
);

// list of universal sessions by session id
create table if not exists viki.user_session (
    id uuid, email text, ip_address text, last_access bigint,
    primary key(id)
);

// list of active sessions for an organisation
create table if not exists viki.active_session (
    organisation_id uuid, id uuid, email text, ip_address text, last_access bigint,
    primary key((organisation_id), id)
);

// list of universal sessions by user email
create table if not exists viki.user_session_by_email (
    email text, id uuid, ip_address text, last_access bigint,
    primary key(email)
);

create table if not exists viki.user_activate_account (
    email text, activation_id uuid, created bigint,
    primary key(email)
);

create table if not exists viki.user_password_reset (
    email text, reset_id uuid, created bigint,
    primary key(email)
);

/////////////////////////////////////////////
// group

create table if not exists viki.group (
	name text, organisation_id uuid,
	user_list list<text>,
	primary key(organisation_id, name)
);

/////////////////////////////////////////////
// document

create table if not exists viki.document_registry (
	organisation_id uuid, url text, acl_list list<text>, processing_pipeline bigint, date_time_uploaded bigint,
	ts_converted bigint, ts_parsed bigint, ts_vectorised bigint, ts_summarised bigint,
	ts_indexed bigint, ts_clustered bigint, ts_entity_analysed bigint, ts_knowledge_analysed bigint,
	ts_emotion_analysed bigint, origin text, title text, author text, created bigint,
	content_hash text, acl_hash int,
	primary key(organisation_id, url)
);

create table if not exists viki.document_binary (
	organisation_id uuid, url text, block_id int, data blob,
	primary key((organisation_id, url), block_id)
);

create table if not exists viki.document_metadata (
	organisation_id uuid, url text, name text, value text,
	primary key((organisation_id, url), name)
);

create table if not exists viki.document_author (
	organisation_id uuid, url text, data blob,
	primary key((organisation_id, url))
);

create table if not exists viki.document_parsed_binary (
	organisation_id uuid, url text, meta_data text, data blob,
	primary key((organisation_id, url), meta_data)
);

create table if not exists viki.document_vector (
	organisation_id uuid, url text, vector_type text, vector text,
	primary key((organisation_id, url), vector_type)
);

create table if not exists viki.document_summarize_word (
	organisation_id uuid, url text, fragment text,
	primary key((organisation_id, url))
);

create table if not exists viki.document_summarize_sentence (
	organisation_id uuid, url text, sentence_json text,
	primary key((organisation_id, url))
);

create table if not exists viki.document_word2vec (
    organisation_id uuid, url text, word text, value_list list<double>,
    primary key((organisation_id, url), word)
);

create table if not exists viki.document_similars (
    organisation_id uuid, url1 text, url2 text, similarity double,
    primary key((organisation_id), url1, url2)
);

/////////////////////////////////////////////
// document images

create table if not exists viki.document_image (
	organisation_id uuid, url text, data blob,
	primary key(organisation_id, url)
);

/////////////////////////////////////////////
// document case tuples (NLU)

// tuples as specified by indexes
create table if not exists viki.tuple (
    organisation_id uuid, id uuid, url text, sentence_id int, json text,
    primary key((organisation_id, id))
);

// access tuples by url
create table if not exists viki.tuple_by_url (
    organisation_id uuid, url text, id uuid,
    primary key((organisation_id, url), id)
);


/////////////////////////////////////////////
// indexes

// generic inverted index: word -> url.  The word_origin is the base of the relationship expansion
// offset is WHERE the word is.  meta_data is what part of the system the document is from
// acl_hash is the security hash for accessibility
create table if not exists viki.word_index (
    organisation_id uuid, word text, shard int, synset int, word_origin text, tag text, url text, meta_data text,
    offset int, meta_c_type int, acl_hash int,
    primary key((organisation_id, word, meta_data, shard), url, word_origin, offset)
);

// emotional indexes.  for a url, give the compound vader value for a given sentence_id (List<ParseTree> index)
create table if not exists viki.emotional_index (
    organisation_id uuid, url text, sentence_id int, value double, acl_hash int,
    primary key((organisation_id, url), sentence_id)
);

// remove indexes, given a url, get the words indexed for that url
create table if not exists viki.word_unindex (
    organisation_id uuid, url text, shard int, word text, meta_data text,
    primary key((organisation_id , url, meta_data), word, shard)
);

// lookup any document by time,  minimum year + month - drill down further by day, hour, minute
// returns a set of urls for those times for a given meta-data
create table if not exists viki.time_index (
    organisation_id uuid, url text, year int, month int, day int, hour int, acl_hash int, offset int,
    primary key((organisation_id, year, month), day, hour, url)
);

/////////////////////////////////////////////
// hazelcast stores

// total counts for words
create table if not exists viki.word_count (
    word text, count bigint,
    primary key(word)
);

// acl sets by hash
create table if not exists viki.acl_set (
    hash int, acl_list list<text>,
    primary key(hash)
);

// number of indexes for a given document
create table if not exists viki.url_index_count (
    organisation_id uuid, url text, index_count bigint,
    primary key((organisation_id, url))
);


/////////////////////////////////////////////
// cluster

// the cluster data minus the urls
create table if not exists viki.cluster_data (
	organisation_id uuid, cluster int, json_data text,
	primary key((organisation_id, cluster))
);

// the url data for the clusters (membership)
create table if not exists viki.cluster_contents (
	organisation_id uuid, cluster int, json_data text,
	primary key((organisation_id, cluster))
);

// is this cluster up-to-date?
// any document parse create/update changes last_changed
// the system itself changes the last_clustered after a cluster run
create table if not exists viki.cluster_up_to_date (
	organisation_id uuid, last_clustered bigint, last_change bigint, last_cosine_change bigint,
	primary key(organisation_id)
);

// the most negative and positive documents
create table if not exists viki.document_emotion (
	organisation_id uuid, url text, score double, sentence_id int,
	primary key((organisation_id), score, url)
);

// the most negative and positive documents
create table if not exists viki.document_anomaly (
	organisation_id uuid, url text,
	primary key((organisation_id), url)
);


/////////////////////////////////////////////
// rules

// the rule system storage
create table if not exists viki.rules (
	organisation_id uuid, rule_name text, json text,
	primary key((organisation_id), rule_name)
);


/////////////////////////////////////////////
// reports

create table if not exists viki.reports (
	organisation_id uuid, report_name text, creator text, report_id int, last_run bigint,
	primary key((organisation_id), report_name)
);


/////////////////////////////////////////////
// neural networks

create table if not exists viki.nnet_training (
	organisation_id uuid, word text, synset_id int, training_data_id uuid, training_data text,
	primary key((organisation_id, word), synset_id)
);

create table if not exists viki.nnet_store (
	organisation_id uuid, word text, last_updated bigint, model_data text,
	primary key((organisation_id, word))
);


/////////////////////////////////////////////
// action/document processing queue

// the rule system storage
create table if not exists viki.document_queue (
	key bigint, organisation_id uuid, url text,
	primary key(key)
);


/////////////////////////////////////////////
// integrated knowledge base

create table if not exists viki.knowledge_base (
	organisation_id uuid, id uuid, type text, origin text, json_data text,
	primary key((organisation_id, type), id)
);

