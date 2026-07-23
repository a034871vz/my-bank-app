package ru.yandex.practicum;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.yandex.practicum.client.GatewayClient;
import ru.yandex.practicum.dto.CashResponse;
import ru.yandex.practicum.dto.TransferResponse;
import ru.yandex.practicum.enums.CashAction;

import java.time.LocalDate;
import java.util.ArrayList;
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

        return "main";
    }

    @PostMapping("/account")
    public String editAccount(Model model, @RequestParam("name") String name, @RequestParam("birthdate") LocalDate birthdate) {
        List<String> errors = new ArrayList<>();

        try {
            gatewayClient.updateAccount(name, birthdate);
            model.addAttribute("info", "Данные сохранены");

        } catch (Exception e) {
            log.error("Ошибка обновления аккаунта: {}", e.getMessage());
            errors.add("Ошибка сохранения: " + e.getMessage());
        }

        return getAccountWithErrors(model, errors);
    }

    @PostMapping("/cash")
    public String editCash(Model model, @RequestParam("value") int value, @RequestParam("action") CashAction action) {
        List<String> errors = new ArrayList<>();

        try {
            String type = action == CashAction.PUT ? "DEPOSIT" : "WITHDRAW";
            CashResponse response = gatewayClient.processCash(type, value);

            model.addAttribute("info", response.message());

        } catch (Exception e) {
            log.error("Ошибка cash операции: {}", e.getMessage());
            errors.add("Ошибка операции: " + extractErrorMessage(e));
        }

        return getAccountWithErrors(model, errors);
    }

    @PostMapping("/transfer")
    public String transfer(Model model, @RequestParam("value") int value, @RequestParam("login") String login) {
        List<String> errors = new ArrayList<>();

        try {
            TransferResponse response = gatewayClient.transfer(login, value);
            model.addAttribute("info", response.message());

        } catch (Exception e) {
            log.error("Ошибка перевода: {}", e.getMessage());
            errors.add("Ошибка перевода: " + extractErrorMessage(e));
        }

        return getAccountWithErrors(model, errors);
    }

    private String getAccountWithErrors(Model model, List<String> errors) {
        try {
            var account = gatewayClient.getMyAccount();
            var accounts = gatewayClient.getAllAccounts();

            model.addAttribute("name", account.name());
            model.addAttribute("birthdate", account.birthdate());
            model.addAttribute("sum", account.balance());
            model.addAttribute("accounts", accounts);
            model.addAttribute("errors", errors.isEmpty() ? null : errors);

        } catch (Exception e) {
            log.error("Ошибка перезагрузки данных: {}", e.getMessage());
            errors.add("Ошибка загрузки данных: " + e.getMessage());
            model.addAttribute("errors", errors);
        }

        return "main";
    }

    private String extractErrorMessage(Exception e) {
        String message = e.getMessage();
        if (message != null && message.contains("error")) {
            try {
                int start = message.indexOf("error:") + 9;
                int end = message.indexOf("", start);
                return message.substring(start, end);
            } catch (Exception ignored) {
            }
        }
        return message;
    }
}
