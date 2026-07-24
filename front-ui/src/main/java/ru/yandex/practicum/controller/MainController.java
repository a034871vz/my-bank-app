package ru.yandex.practicum.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.yandex.practicum.client.GatewayClient;
import ru.yandex.practicum.dto.CashResponse;
import ru.yandex.practicum.dto.TransferResponse;
import ru.yandex.practicum.enums.CashAction;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
public class MainController {

    private final GatewayClient gatewayClient;

    @GetMapping
    public String index() {
        return "redirect:/account";
    }

    @GetMapping("/account")
    public String getAccount(Model model) {
        populateAccountModel(model);
        return "main";
    }

    @PostMapping("/account")
    public String editAccount(@RequestParam("name") String name, @RequestParam("birthdate") LocalDate birthdate,
                              RedirectAttributes redirectAttributes) {

        if (birthdate == null || birthdate.isAfter(LocalDate.now().minusYears(18))) {
            log.warn("Попытка регистрации/обновления аккаунта пользователем младше 18 лет: birthdate={}", birthdate);
            redirectAttributes.addFlashAttribute("errors", List.of("Возраст должен быть не менее 18 лет"));
            return "redirect:/account";
        }

        try {
            gatewayClient.updateAccount(name, birthdate);
            redirectAttributes.addFlashAttribute("info", "Данные сохранены");
        } catch (Exception e) {
            log.error("Ошибка обновления аккаунта: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errors", List.of("Ошибка сохранения: " + e.getMessage()));
        }

        return "redirect:/account";
    }

    @PostMapping("/cash")
    public String editCash(@RequestParam("value") int value, @RequestParam("action") CashAction action, RedirectAttributes redirectAttributes) {
        try {
            String type = action == CashAction.PUT ? "DEPOSIT" : "WITHDRAW";
            CashResponse response = gatewayClient.processCash(type, value);
            redirectAttributes.addFlashAttribute("info", response.message());
        } catch (Exception e) {
            log.error("Ошибка cash операции: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errors", List.of("Ошибка операции: " + e.getMessage()));
        }

        return "redirect:/account";
    }

    @PostMapping("/transfer")
    public String transfer(@RequestParam("value") int value, @RequestParam("login") String login, RedirectAttributes redirectAttributes) {
        try {
            TransferResponse response = gatewayClient.transfer(login, value);
            redirectAttributes.addFlashAttribute("info", response.message());
        } catch (Exception e) {
            log.error("Ошибка перевода: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errors", List.of("Ошибка перевода: " + e.getMessage()));
        }

        return "redirect:/account";
    }

    private void populateAccountModel(Model model) {
        try {
            var account = gatewayClient.getMyAccount();
            var accounts = gatewayClient.getAllAccounts();

            model.addAttribute("name", account.name());
            model.addAttribute("birthdate", account.birthdate());
            model.addAttribute("sum", account.balance());
            model.addAttribute("accounts", accounts);
        } catch (Exception e) {
            log.error("Ошибка получения аккаунта: {}", e.getMessage());
            model.addAttribute("errors", List.of("Ошибка загрузки данных: " + e.getMessage()));
        }
    }
}