package com.kakarote.authorization.config.password;

import com.kakarote.authorization.common.AuthException;
import com.kakarote.authorization.common.AuthorizationCodeEnum;
import com.kakarote.authorization.entity.AuthorizationUser;
import com.kakarote.authorization.entity.AuthorizationUserInfo;
import com.kakarote.core.entity.UserInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.AbstractUserDetailsAuthenticationProvider;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;


/**
 * @author z
 * 认证相关代码
 */
@Slf4j
public class AuthenticationProvider extends AbstractUserDetailsAuthenticationProvider {

    @Autowired
    private UserDetailsService userDetailsService;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    protected void additionalAuthenticationChecks(UserDetails userDetails, UsernamePasswordAuthenticationToken authentication) throws AuthenticationException {
        AuthorizationUser user;
        if (userDetails instanceof AuthorizationUser) {
            user = (AuthorizationUser) userDetails;
        } else {
            logger.debug("Authentication failed: no credentials provided");
            throw new AuthException(AuthorizationCodeEnum.AUTHORIZATION_LOGIN_ERR);
        }
        // 如果token的相关认证信息为nul 则抛出错误
        if (authentication.getCredentials() == null) {
            logger.debug("Authentication failed: no credentials provided");
            throw new AuthException(AuthorizationCodeEnum.AUTHORIZATION_LOGIN_ERR);
        }
        String presentedPassword = authentication.getCredentials().toString();
        List<UserInfo> userInfoList = user.getUserInfoList();
        if (userInfoList.size() == 0) {
            throw new AuthException(AuthorizationCodeEnum.AUTHORIZATION_LOGIN_NO_USER);
        }
        AuthorizationUserInfo userDetailsInfo = new AuthorizationUserInfo();

        userInfoList.forEach(userInfo -> {
            AuthorizationUser authorizationUser = AuthorizationUser.toAuthorizationUser(userInfo);
            if (passwordEncoder.matches(presentedPassword, authorizationUser.toJSON())) {
                userDetailsInfo.addAuthorizationUser(authorizationUser);
            }
           // userDetailsInfo.addAuthorizationUser(authorizationUser);
//            if(!user.getIsOssLogin()){
//                userDetailsInfo.addAuthorizationUser(authorizationUser);
//            }

        });
        log.error("验证过密码后的信息{}",userDetailsInfo);
        authentication.setDetails(userDetailsInfo);
    }
//AuthorizationUser(smscode=null, password=138604c01802f3f891df757870b5d5e8, salt=76a1596e73764214add191cac051f72f, isOssLogin=false, loginType=1, type=1, userInfoList=[])])
//2021-08-15 20:30:48 authorization INFO  com.kakarote.authorization.common.LoginLogUtil - ===> Login IP : 127.0.0.1
    @Override
    protected UserDetails retrieveUser(String username, UsernamePasswordAuthenticationToken authentication) throws AuthenticationException {
        try {
            UserDetails loadedUser = userDetailsService.loadUserByUsername(username);
            if (loadedUser == null) {
                throw new InternalAuthenticationServiceException("UserDetailsService returned null, which is an interface contract violation");
            }
            return loadedUser;
        } catch (UsernameNotFoundException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new InternalAuthenticationServiceException(ex.getMessage(), ex);
        }
    }
}
