select 
  ph.log_context
 ,ph.message_ref
 ,ph.client_ip
 ,ph.client_port
 ,ph.remote_hostname
 ,ph.remote_ip
 ,ph.remote_port
 ,ph.full_url
 ,ph.request_scheme
 ,ph.request_method
 ,ph.request_url
 ,ph.request_protocol_version
 ,ph.request_host_header
 ,ph.request_charset
 ,ph.response_status_code
 ,ph.response_charset
 ,ph.at_client_to_proxy
 ,ph.at_proxy_to_server_connect_start
 ,ph.at_proxy_to_server_connected
 ,ph.at_proxy_to_server_request_sent
 ,ph.at_server_to_proxy_response_recv
 ,ph.elapsed_time_connect
 ,ph.elapsed_time_req_sent
 ,ph.elapsed_time_res_recv
 ,ph.elapsed_time_full
 ,ph.comment
 ,sch_join.sch_count as sch_count

from proxy_history as ph 

left outer join (
  select
    sch.log_context
   ,sch.message_ref
   ,count(sch.sc_id) as sch_count
  from screenshot_history as sch
  group by sch.log_context, sch.message_ref
) as sch_join
  on ph.log_context = sch_join.log_context and
     ph.message_ref = sch_join.message_ref

order by ph.at_client_to_proxy desc
