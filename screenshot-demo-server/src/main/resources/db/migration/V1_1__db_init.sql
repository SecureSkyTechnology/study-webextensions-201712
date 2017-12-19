create table proxy_history(
log_context varchar not null,
message_ref bigint not null,

client_ip varchar not null,
client_port int not null,
remote_hostname varchar not null,
remote_ip varchar not null,
remote_port int not null,

full_url varchar not null,

request_scheme varchar not null,
request_method varchar not null,
request_url varchar not null,
request_protocol_version varchar not null,
request_host_header varchar,
request_bytes binary,
request_charset varchar default 'UTF-8',

response_status_code smallint default 0, 
response_bytes binary,
response_charset varchar default 'UTF-8',

at_client_to_proxy timestamp,
at_proxy_to_server_connect_start timestamp,
at_proxy_to_server_connected timestamp,
at_proxy_to_server_request_sent timestamp,
at_server_to_proxy_response_recv timestamp,
elapsed_time_connect int default 0,
elapsed_time_req_sent int default 0,
elapsed_time_res_recv int default 0,
elapsed_time_full int default 0,

comment varchar,
primary key (log_context, message_ref)
);

create table screenshot_history(
log_context varchar not null,
message_ref bigint not null,
sc_id bigint not null,
screenshot_url varchar not null,
screenshot_src varchar not null,
created_at timestamp,
comment varchar,
primary key (log_context, sc_id),
foreign key (log_context, message_ref) references proxy_history(log_context, message_ref)
)
