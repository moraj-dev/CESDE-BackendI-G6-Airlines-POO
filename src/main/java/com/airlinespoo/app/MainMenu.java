package com.airlinespoo.app;

import com.airlinespoo.core.ui.ConsoleInput;
import com.airlinespoo.core.ui.Menu;
import com.airlinespoo.core.util.Validator;

import java.util.List;

/**
 * Menú principal de Airlines-poo que agrupa los cinco módulos del sistema.
 */
public class MainMenu implements Menu {

    private final List<Menu> modules;
    private final ConsoleInput input;

    public MainMenu(List<Menu> modules, ConsoleInput input) {
        this.modules = List.copyOf(Validator.requireNotNull(modules, "lista de módulos"));
        this.input = Validator.requireNotNull(input, "lector de consola");
    }

    @Override
    public String title() {
        return "Menú Principal - Airlines-poo";
    }

    @Override
    public void run() {
        boolean exit = false;
        while (!exit) {
            System.out.println();
            System.out.println("=================================================");
            System.out.println("            AIRLINES-POO · MENÚ PRINCIPAL        ");
            System.out.println("=================================================");
            for (int i = 0; i < modules.size(); i++) {
                System.out.printf("%d. %s%n", i + 1, modules.get(i).title());
            }
            System.out.println("0. Salir");

            int option = input.readInt("Seleccione un módulo", 0, modules.size());
            if (option == 0) {
                exit = true;
            } else {
                modules.get(option - 1).run();
            }
        }
    }
}
