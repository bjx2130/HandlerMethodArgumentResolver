package com.huawei.cloudopenlabs.portal.config.argResolver;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;

import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huawei.cloudopenlabs.portal.data.entity.RequestParamVO;

@Component
public class VarIntoRequestBodyArgumentResolver implements HandlerMethodArgumentResolver{
	
	private ObjectMapper objectMapper;
	
	public VarIntoRequestBodyArgumentResolver(ObjectMapper objectMapper){
		this.objectMapper = objectMapper;
	}
	
	
	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.hasParameterAnnotation(VarIntoRequestBody.class);	
	}

	@Override
	public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
		
		Map<String,String> uriTemplateVariables = (Map<String, String>)webRequest.getAttribute("org.springframework.web.servlet.HandlerMapping.uriTemplateVariables", RequestAttributes.SCOPE_REQUEST);
		Map<String,String[]> paramVariables = webRequest.getParameterMap();
		HttpServletRequest request  = webRequest.getNativeRequest(HttpServletRequest.class);
		BufferedReader br = new BufferedReader( new InputStreamReader(request.getInputStream()));
		StringBuffer httpBody = new StringBuffer();
		String line =  br.readLine();
		while(line!=null){
			httpBody.append(line);
			line =  br.readLine();
		}
		
		Class<?> clazz = parameter.getParameterType();
		Object target = this.objectMapper.readValue(httpBody.toString(), clazz);
		Iterator<Entry<String, String>> pathVarIter = uriTemplateVariables.entrySet().iterator();
		Entry<String, String> ent = null;
		Method method = null;
		Field fd = null;
		while(pathVarIter.hasNext()){
			ent = pathVarIter.next();
			fd = ReflectionUtils.findField(clazz, ent.getKey());
			if(fd==null)
					continue;
			method = ReflectionUtils.findMethod(clazz, "set"+this.captureName(ent.getKey()),fd.getType());
			if(method==null)
					continue;
			
			ReflectionUtils.invokeMethod(method, target,ent.getValue());
		}
		return target;
	}
	
	
	public static String captureName(String name) {
       return  name.substring(0, 1).toUpperCase() + name.substring(1);
    }
}
