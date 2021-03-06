package com.kakarote.authorization.service.impl;

import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson.JSONObject;
import com.kakarote.authorization.common.AuthException;
import com.kakarote.authorization.common.AuthorizationCodeEnum;
import com.kakarote.authorization.common.LoginLogUtil;
import com.kakarote.authorization.entity.AdminUserStatusBO;
import com.kakarote.authorization.entity.AuthorizationUser;
import com.kakarote.authorization.entity.AuthorizationUserInfo;
import com.kakarote.authorization.entity.VO.LoginVO;
import com.kakarote.authorization.service.AdminUserService;
import com.kakarote.authorization.service.LoginService;
import com.kakarote.core.common.LoginType;
import com.kakarote.core.common.Result;
import com.kakarote.core.common.SystemCodeEnum;
import com.kakarote.core.common.cache.AdminCacheKey;
import com.kakarote.core.entity.UserInfo;
import com.kakarote.core.exception.CrmException;
import com.kakarote.core.feign.admin.entity.LoginLogEntity;
import com.kakarote.core.feign.admin.service.LogService;
import com.kakarote.core.redis.Redis;
import com.kakarote.core.servlet.ApplicationContextHolder;
import com.kakarote.core.utils.UserUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.math.NumberUtils;
import org.codehaus.stax2.ri.typed.NumberUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

/**
 * @author Administrator
 */
@Slf4j
@Service
public class UserDetailsServiceImpl implements UserDetailsService, LoginService {

    @Autowired
    private AdminUserService adminUserService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private Redis redis;


    @Autowired
    private LoginLogUtil loginLogUtil;


    @Override
    @SuppressWarnings("unchecked")
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Result result = adminUserService.findByUsername(username);
        if (result.hasSuccess()) {
            return new AuthorizationUser().setUserInfoList((List<Object>) result.getData());
        }
        throw new UsernameNotFoundException(null);
    }

    /**
     * ?????????????????????
     *
     * @param user ????????????
     * @param request
     * @return Result
     */
    @Override
    public Result login(AuthorizationUser user, HttpServletResponse response, HttpServletRequest request) {
        LoginLogEntity logEntity = loginLogUtil.getLogEntity(request);
        String token = IdUtil.simpleUUID();
        UserInfo userInfo = user.toUserInfo();
        logEntity.setUserId(userInfo.getUserId());
        logEntity.setRealname(userInfo.getRealname());
        if (userInfo.getStatus() == 0) {
            logEntity.setAuthResult(2);
            logEntity.setFailResult(AuthorizationCodeEnum.AUTHORIZATION_USER_DISABLE_ERROR.getMsg());
            ApplicationContextHolder.getBean(LogService.class).saveLoginLog(logEntity);
            throw new CrmException(AuthorizationCodeEnum.AUTHORIZATION_USER_DISABLE_ERROR);
        }
        userInfo.setRoles(adminUserService.queryUserRoleIds(userInfo.getUserId()).getData());
        UserUtil.userToken(token, userInfo, user.getType());
        if (userInfo.getStatus() == 2) {
            adminUserService.setUserStatus(AdminUserStatusBO.builder().status(1).ids(Collections.singletonList(userInfo.getUserId())).build());
        }
        ApplicationContextHolder.getBean(LogService.class).saveLoginLog(logEntity);
        return Result.ok(new LoginVO().setAdminToken(token));
    }

    /**
     * ?????????????????????
     *
     * @param user ????????????
     * @return Result
     */
    @Override
    public Result doLogin(AuthorizationUser user, HttpServletResponse response,HttpServletRequest request) {
        LoginType loginType = LoginType.valueOf(user.getLoginType());

        if (loginType.equals(LoginType.PASSWORD) ){
            String key = AdminCacheKey.PASSWORD_ERROR_CACHE_KEY + user.getUsername().trim();
            Integer errorNum = redis.get(key);
            if (errorNum != null && errorNum > 2) {
                Integer second = Optional.ofNullable(redis.ttl(key)).orElse(0L).intValue();
                if (second > 0) {
                    String errorTimeDesc = this.getErrorTimeDesc(second);
                    return Result.error(AuthorizationCodeEnum.AUTHORIZATION_LOGIN_PASSWORD_TO_MANY_ERROR, "?????????????????????????????????"+errorTimeDesc+"????????????");
                }
            }
        }
        try {
            AbstractAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(user.getUsername().trim(), user.getPassword().trim());
            Authentication authentication = authenticationManager.authenticate(authenticationToken);
            AuthorizationUserInfo userInfo = (AuthorizationUserInfo) authentication.getDetails();
            if (userInfo.getAuthorizationUserList().size() == 0) {
                return this.handleLoginPassWordToManyError(user.getUsername().trim());
            }
            return login(userInfo.getAuthorizationUserList().get(0).setType(user.getType()), response, request);
        } catch (AuthException e) {
            return Result.error(e.getResultCode());
        } catch (BadCredentialsException e) {
            return this.handleLoginPassWordToManyError(user.getUsername().trim());
        }
    }





    /**
     * oss????????????
     *
     * @param userName
     *                  ?????????
     *
     *
     * @return Result
     */
    @Override
    public Result ossLogin(String userName, HttpServletResponse response,HttpServletRequest request) {

        Result result = adminUserService.findByUsername(userName);
        List<Map<String, Object>> userList = (List<Map<String, Object>>) result.getData();
        if(null != userList && userList.size() > 0)
        {
            AuthorizationUser user = new AuthorizationUser();
            user.setUsername(userName);
            user.setPassword(userList.get(0).get("password").toString());
            user.setIsOssLogin(true);
            user.setType(1);
            System.out.println(JSONObject.toJSON(userList.get(0)));
            UserInfo userInfo = new UserInfo();
            userInfo.setUserId(NumberUtils.toLong(userList.get(0).get("userId").toString()));
            userInfo.setRealname(userList.get(0).get("realname").toString());
            userInfo.setStatus(NumberUtils.toInt((userList.get(0).get("status").toString())));
            userInfo.setUsername(userName);
            userInfo.setDeptId(NumberUtils.toInt(userList.get(0).get("deptId").toString()));
            userInfo.setLoginType(1);
            userInfo.setRequest(request);
            userInfo.setResponse(response);
            UserUtil.setUser(userInfo);
            return loginEx(user, userInfo, response, request);
        }
        else
        {
            Result.error(302, "??????????????????");
        }
        return Result.error(302, "??????????????????");
    }

    @Override
    public Result loginEx(AuthorizationUser user, UserInfo userInfo, HttpServletResponse response, HttpServletRequest request) {
        LoginLogEntity logEntity = loginLogUtil.getLogEntity(request);
        String token = IdUtil.simpleUUID();
        logEntity.setUserId(userInfo.getUserId());
        logEntity.setRealname(userInfo.getRealname());
        if (userInfo.getStatus() == 0) {
            logEntity.setAuthResult(2);
            logEntity.setFailResult(AuthorizationCodeEnum.AUTHORIZATION_USER_DISABLE_ERROR.getMsg());
            ApplicationContextHolder.getBean(LogService.class).saveLoginLog(logEntity);
            throw new CrmException(AuthorizationCodeEnum.AUTHORIZATION_USER_DISABLE_ERROR);
        }
        userInfo.setRoles(adminUserService.queryUserRoleIds(userInfo.getUserId()).getData());
        UserUtil.userToken(token, userInfo, user.getType());
        if (userInfo.getStatus() == 2) {
            adminUserService.setUserStatus(AdminUserStatusBO.builder().status(1).ids(Collections.singletonList(userInfo.getUserId())).build());
        }
        ApplicationContextHolder.getBean(LogService.class).saveLoginLog(logEntity);
        return Result.ok(new LoginVO().setAdminToken(token));
    }

    /**
     * ????????????
     * @date 2020/11/9 16:57
     * @param second
     * @return java.lang.String
     **/
    private String getErrorTimeDesc(Integer second){
        String errorTimeDesc;
        if (Arrays.asList(300, 240, 180, 120, 60).contains(second)) {
            errorTimeDesc = second / 60 + "???";
        } else if (second < 60) {
            errorTimeDesc = second + "???";
        } else {
            errorTimeDesc = second / 60 + "???" + second % 60 + "???";
        }
        return errorTimeDesc;
    }

    /**
     * ????????????????????????
     * @date 2020/11/9 15:42
     * @param userName
     * @return com.kakarote.core.common.Result
     **/
    private Result handleLoginPassWordToManyError(String userName){
        String key = AdminCacheKey.PASSWORD_ERROR_CACHE_KEY + userName;
        Integer errorNum = redis.get(key);
        if (errorNum == null) {
            redis.setex(AdminCacheKey.PASSWORD_ERROR_CACHE_KEY + userName, 60 * 3, 1);
        }else if(errorNum < 3){
            Integer defineTime = errorNum == 2 ? 60 * 2 : 60 * 3;
            redis.setex(AdminCacheKey.PASSWORD_ERROR_CACHE_KEY + userName, defineTime, errorNum + 1);
        }
        return Result.error(AuthorizationCodeEnum.AUTHORIZATION_LOGIN_NO_USER);
    }

    @Override
    public Result permission(String authentication, String url, String method) {
        UserInfo userInfo = redis.get(authentication);
        if (userInfo == null) {
            throw new CrmException(SystemCodeEnum.SYSTEM_NOT_LOGIN);
        }
        Long userId = userInfo.getUserId();
        String key = userId.toString();
        List<String> noAuthMenuUrls = redis.get(key);
        if (noAuthMenuUrls == null) {
            noAuthMenuUrls = adminUserService.queryNoAuthMenu(userId).getData();
        }
        boolean permission = isHasPermission(noAuthMenuUrls, url);
        return permission ? Result.ok() : Result.noAuth();
    }

    @Override
    public Result logout(String authentication) {
        Object data = redis.get(authentication);
        if (data instanceof UserInfo) {
            UserInfo userInfo = (UserInfo) data;
            redis.del(authentication);
            redis.del(userInfo.getUserId());
            redis.del(AdminCacheKey.USER_AUTH_CACHE_KET+userInfo.getUserId());
        }
        return Result.ok();
    }


    /**
     * ??????????????????????????????
     *
     * @param noAuthMenuUrls
     * @param url
     * @return boolean
     * @date 2020/8/21 13:35
     **/
    private boolean isHasPermission(List<String> noAuthMenuUrls, String url) {
        //?????????????????? | ??????
        if (noAuthMenuUrls == null) {
            return false;
        }
        //?????????
        if (noAuthMenuUrls.size() == 0) {
            return true;
        }
        //??????????????????
        if (noAuthMenuUrls.size() == 1 && "/*/**".equals(noAuthMenuUrls.get(0))) {
            return false;
        }
        boolean permission = true;
        for (String noAuthMenuUrl : noAuthMenuUrls) {
            if (noAuthMenuUrl.contains("*")) {
                if (noAuthMenuUrl.contains(",")) {
                    boolean isNoAuth = false;
                    for (String noAuthUrl : noAuthMenuUrl.split(",")) {
                        if (url.startsWith(noAuthUrl.replace("*", ""))) {
                            isNoAuth = true;
                            break;
                        }
                    }
                    if (isNoAuth) {
                        permission = false;
                        break;
                    }
                } else {
                    if (url.startsWith(noAuthMenuUrl.replace("*", ""))) {
                        permission = false;
                        break;
                    }
                }
            } else {
                if (noAuthMenuUrl.contains(",")) {
                    if (Arrays.asList(noAuthMenuUrl.split(",")).contains(url)) {
                        permission = false;
                        break;
                    }
                } else {
                    if (noAuthMenuUrl.equals(url)) {
                        permission = false;
                        break;
                    }
                }
            }
        }
        return permission;
    }
}
