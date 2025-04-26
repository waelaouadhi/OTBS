//package com.otbs.notification.security;
//import com.otbs.feign.client.EmployeeClient;
//import com.otbs.feign.dto.EmployeeResponse;
//import com.otbs.notification.util.JwtUtils;
//import lombok.RequiredArgsConstructor;
//import org.springframework.messaging.Message;
//import org.springframework.messaging.MessageChannel;
//import org.springframework.messaging.simp.stomp.StompCommand;
//import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
//import org.springframework.messaging.support.ChannelInterceptor;
//import org.springframework.messaging.support.MessageHeaderAccessor;
//import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
//import org.springframework.security.core.authority.SimpleGrantedAuthority;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.stereotype.Component;
//
//import java.util.List;
//import java.util.stream.Collectors;
//@Component
//@RequiredArgsConstructor
//public class WebSocketAuthenticationInterceptor implements ChannelInterceptor {
//
//    private final JwtUtils jwtUtils;
//    private final EmployeeClient employeeClient;
//    @Override
//    public Message<?> preSend(Message<?> message, MessageChannel channel) {
//        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
//        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
//            String jwt = accessor.getFirstNativeHeader("Authorization");
//            if (jwt != null && jwt.startsWith("Bearer ")) {
//                jwt = jwt.substring(7);
//                if (jwtUtils.validateJwtToken(jwt)) {
//                    String username = jwtUtils.getUserNameFromJwtToken(jwt);
//                    List<String> roles = jwtUtils.getRolesFromJwtToken(jwt);
//                    EmployeeResponse user = employeeClient.getEmployeeByUsername(username);
//
//                    List<SimpleGrantedAuthority> authorities = roles.stream()
//                            .map(SimpleGrantedAuthority::new)
//                            .collect(Collectors.toList());
//
//                    UsernamePasswordAuthenticationToken authentication =
//                            new UsernamePasswordAuthenticationToken(
//                                    user,
//                                    null,
//                                    authorities
//                            );
//
//                    SecurityContextHolder.getContext().setAuthentication(authentication);
//                    accessor.setUser(authentication);
//                }
//            }
//        }
//        return message;
//    }
//}