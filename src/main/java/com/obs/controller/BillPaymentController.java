package com.obs.controller;

import com.obs.entity.BillPayment;
import com.obs.entity.User;
import com.obs.payload.response.MessageResponse;


import com.obs.service.Interfaces.IBillPaymentService;
import com.obs.service.Interfaces.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/bills")
public class BillPaymentController {

    @Autowired
    private IBillPaymentService billPaymentService;

    @Autowired
    private IUserService userService;

    @PostMapping("/pay")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> payBill(@RequestParam String accountNumber, 
                                     @RequestParam String billerName, 
                                     @RequestParam BigDecimal amount, 
                                     Principal principal) {
        User user = userService.getByUsername(principal.getName());
        billPaymentService.payBill(user.getId(), accountNumber, billerName, amount);
        return ResponseEntity.ok(new MessageResponse("Bill paid successfully!"));
    }

    @GetMapping("/my-bills")
    @PreAuthorize("hasRole('CUSTOMER')")
    public List<BillPayment> getMyBills(Principal principal) {
        User user = userService.getByUsername(principal.getName());
        return billPaymentService.getMyBills(user.getId());
    }
}
