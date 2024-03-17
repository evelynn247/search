package com.biyao.search.as.server.common.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSON;
import com.biyao.search.as.service.model.request.SearchRequest;
import com.biyao.search.common.model.RPCResult;
import com.biyao.search.common.model.Status;

@Aspect
@Component
public class ExceptionCacheAOP{
	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	@Pointcut(value = "execution(* com.biyao.search.as.server.remote.ASMainSearchServiceImpl.*(..))")
	public void pointcut(){}
	
	@Around("pointcut() && args(request)")
	public Object around(ProceedingJoinPoint joinPoint, SearchRequest request) {
		try {
			return joinPoint.proceed();
		} catch (Throwable e) {
			logger.error("[未知异常]searchas发生不可预知异常，调用方法：{}，request:{}", joinPoint.getSignature().getName(), JSON.toJSONString(request), e);
			return new RPCResult<>(new Status(10000, "search as发生不可预知异常"));
		}
	}
}
