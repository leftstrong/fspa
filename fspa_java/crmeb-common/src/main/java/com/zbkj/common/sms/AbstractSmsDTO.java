package com.zbkj.common.sms;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.apache.commons.beanutils.PropertyUtilsBean;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import java.beans.PropertyDescriptor;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 短信业务抽象类  子类OrderDeliveredSmsDTO.java  注释较完整
 */
@Getter
@Setter
@Accessors(chain = true)
public abstract class AbstractSmsDTO {

    /**
     * 电话号码  构造子类对象时 最后set该属性
     */
    private String telephone;

    /**
     * 业务类型，子类构造方法传入
     */
    private String businessType;

    /** 批量的电话号码 */
    private List<String> telephones;


    public Map<String, String> convertToMap() throws Exception {
        return toStringMap(this);
    }
    public AbstractSmsDTO setTelephones(List<String> telephones) throws Exception {
        if (CollectionUtils.isNotEmpty(telephones) && telephones.size()>1000) {
            throw new Exception("批量发送短信，单次手机号码上限为1000，当前手机号码数量为："+telephones.size());
        }
        this.telephones = telephones;
        return this;
    }


    private static Map toStringMap(Object object) throws Exception{
        Map<String, String> params = new HashMap<>();
        try {
            PropertyUtilsBean propertyUtilsBean = new PropertyUtilsBean();
            PropertyDescriptor[] propertyDescriptors = propertyUtilsBean.getPropertyDescriptors(object);
            for(PropertyDescriptor descriptor : propertyDescriptors){
                String name = descriptor.getName();
                if (StringUtils.equals("class", name)){
                    continue;
                }
                if (StringUtils.equals("templateCode", name)){
                    continue;
                }
                if (StringUtils.equals("telephone", name)){
                    continue;
                }
                if (StringUtils.equals("telephones", name)){
                    continue;
                }
                if (StringUtils.equals("businessType", name)){
                    continue;
                }
                if (StringUtils.equals("BUSINESS_TYPE", name)){
                    continue;
                }
                Object value = propertyUtilsBean.getNestedProperty(object, name);
                if (value != null){
                    params.put(name, value.toString());
                }
            }
            return params;
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }
    }

    protected void setBusinessType(String businessType) {
        this.businessType = businessType;
    }

}
