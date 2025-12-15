/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.shakilpatel.org.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.sql.SQLException;
import org.postgresql.util.PGobject;

/**
 *
 * @author Acer
 */
@Converter(autoApply = true)
public class ApprovalStatusConverter implements AttributeConverter<ApprovalStatus, Object> {

    @Override
    public Object convertToDatabaseColumn(ApprovalStatus attribute) {
        if (attribute == null) {
            return null;
        }

        return new PGobject(){
            {
                setType("approval_status");
                try {
                    setValue(attribute.name());
                } catch (SQLException ex) {
                    System.getLogger(ApprovalStatusConverter.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
                }
            }
        };
    }

    @Override
    public ApprovalStatus convertToEntityAttribute(Object dbData) {
        return dbData == null ? null : ApprovalStatus.valueOf(dbData.toString());
    }
}
