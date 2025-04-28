update requests set authorization_id = null where id in (
    SELECT id from requests
    where authorization_id in (
        select id from request_authorizations
        where id not in(
            select id from oauth2_request_authorizations ora
        ) and type = 'OAUTH2'
    )
)