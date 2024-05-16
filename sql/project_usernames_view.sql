CREATE VIEW project_usernames AS SELECT o_identity.object_id_identity as projectId, u.userName FROM acl_entry 
	LEFT JOIN (SELECT * FROM acl_sid as sid WHERE sid.principal = 1) as sid ON acl_entry.sid = sid.id 
    LEFT JOIN users u ON sid.sid = u.id 
    LEFT JOIN acl_object_identity o_identity ON acl_entry.acl_object_identity = o_identity.id
    WHERE sid.sid IS NOT NULL 
    AND acl_entry.mask != 16;