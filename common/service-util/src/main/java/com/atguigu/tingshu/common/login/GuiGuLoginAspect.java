package com.atguigu.tingshu.common.login;


import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.result.ResultCodeEnum;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
@Slf4j
public class GuiGuLoginAspect {

    @Autowired
    private RedisTemplate redisTemplate;

    @Around("execution(* com.atguigu.tingshu.*.api.*.*(..)) && @annotation(guiGuLogin)")
    public Object loginCheck(ProceedingJoinPoint pjp, GuiGuLogin guiGuLogin) throws Throwable {

        log.info("前置通知");
        //1.从请求头中获取到token
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        ServletRequestAttributes attributes = (ServletRequestAttributes) requestAttributes;
        HttpServletRequest request = attributes.getRequest();
        String token = request.getHeader("token");

        //2.从redis中获取到用户的信息
        //2.1组装key来查询用户的信息
        String key = RedisConstant.USER_LOGIN_KEY_PREFIX + token;
        UserInfoVo userInfoVo = (UserInfoVo) redisTemplate.opsForValue().get(key);
        if (userInfoVo == null && guiGuLogin.required()) {
            throw new GuiguException(ResultCodeEnum.LOGIN_AUTH);
        }
        if (userInfoVo != null) {
            AuthContextHolder.setUserId(userInfoVo.getId());
        }
        Object result = pjp.proceed();
        //从threadlocal中移除用户的id防止内存溢出
        AuthContextHolder.removeUserId();

        log.info("后置通知");
        return result;

    }
}
