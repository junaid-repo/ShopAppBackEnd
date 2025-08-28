/*
 * package com.management.shop.controller;
 * 
 * import org.springframework.beans.factory.annotation.Autowired; import
 * org.springframework.security.authentication.AuthenticationManager; import
 * org.springframework.security.authentication.
 * UsernamePasswordAuthenticationToken; import
 * org.springframework.security.core.Authentication; import
 * org.springframework.security.core.userdetails.UsernameNotFoundException;
 * import org.springframework.validation.annotation.Validated; import
 * org.springframework.web.bind.annotation.PostMapping; import
 * org.springframework.web.bind.annotation.RequestBody; import
 * org.springframework.web.bind.annotation.RequestMapping; import
 * org.springframework.web.bind.annotation.RestController;
 * 
 * import com.management.shop.dto.AuthRequest; import
 * com.management.shop.entity.UserInfo; import
 * com.management.shop.service.JwtService; import
 * com.management.shop.service.ShopService;
 * 
 * @RestController
 * 
 * @RequestMapping("/auth")
 * 
 * @Validated public class AuthController {
 * 
 * @Autowired private JwtService jwtService;
 * 
 * @Autowired private AuthenticationManager authenticationManager;
 * 
 * @Autowired ShopService serv;
 * 
 * @PostMapping("/new/user") public String addNewUser(@RequestBody UserInfo
 * userInfo) { return serv.addUser(userInfo); }
 * 
 * @PostMapping("/token") public String authenticateAndGetToken(@RequestBody
 * AuthRequest authRequest) { Authentication authentication =
 * authenticationManager.authenticate( new
 * UsernamePasswordAuthenticationToken(authRequest.getUsername(),
 * authRequest.getPassword())); if (authentication.isAuthenticated()) { String
 * token= jwtService.generateToken(authRequest.getUsername());
 * //System.out.println("The generated token --> "+token); return token; } else
 * { throw new UsernameNotFoundException("invalid user request !"); }
 * 
 * } } }
 */