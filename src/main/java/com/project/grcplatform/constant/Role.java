package com.project.grcplatform.constant;


// TODO : Move this To The New "constant" Folder
public enum Role {
    SUPER_ADMIN("SUPER_ADMIN"),
    ADMIN("ADMIN"),
    USER("USER"),
    MANAGER("MANAGER"),
    RSSI("RSSI"),
    AUDITEUR("AUDITEUR");

    private final String value;
    Role(String value){
        this.value=value;
    }
    public String getValue(){
        return value;
    }
}
