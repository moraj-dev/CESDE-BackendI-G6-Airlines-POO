package com.airlinespoo.customers.ui;

import com.airlinespoo.core.exception.AirlinesException;
import com.airlinespoo.core.ui.ConsoleInput;
import com.airlinespoo.core.ui.Menu;
import com.airlinespoo.customers.model.Country;
import com.airlinespoo.customers.model.Customer;
import com.airlinespoo.customers.model.DocumentType;
import com.airlinespoo.customers.service.CustomerService;

import java.util.List;


//Se implementa la interfaz Menu del kit base
public class CustomerMenu implements Menu {

    private final CustomerService customerService;
    private final ConsoleInput consoleInput;

    //No se usa new Scanner para evitar errores en saltos de linea
    public CustomerMenu (CustomerService customerService, ConsoleInput consoleInput){
        this.customerService = customerService;
        this.consoleInput = consoleInput;
    }

    @Override
    public String title(){
        return "Gestión de Clientes";
    }

    @Override
    public void run(){
        boolean salir = false;
        //Menu
        while (!salir){
            System.out.println("\n=== " + title().toUpperCase() + " ===");
            System.out.println("1. Registrar Cliente");
            System.out.println("2. Consultar cliente por documento");
            System.out.println("3. Actualizar contacto de cliente");
            System.out.println("4. Listar todos los clientes");
            System.out.println("5. Buscar clientes por apellido");
            System.out.println("6. Eliminar cliente");
            System.out.println("7. Salir");

            int option = consoleInput.readInt("Selecciona una opción: ", 1, 7);

            //try - catch para la opcion seleccionada
            try {
                switch (option){
                    case 1 -> registrarCliente();
                    case 2 -> consultarCliente();
                    case 3 -> actualizarContacto();
                    case 4 -> listarClientes();
                    case 5 -> buscarPorApellido();
                    case 6 -> eliminarCliente ();
                    case 7 -> salir = true;
                }
            }catch (AirlinesException exception){
                System.out.println("❌ Error de regla de negocio " + exception.getMessage());
            }catch (Exception exception){
                System.out.println("❌ Error inesperado: " + exception.getMessage());
            }

            if (!salir){
                consoleInput.pause();
            }
        }
    }

    //Opcion 1. Registro de Clientes
    private void registrarCliente(){
        System.out.println("\n-- REGISTRAR CLIENTE --");

        String firsName = consoleInput.readRequiredText("Ingrese los nombres del cliente");
        String lastName = consoleInput.readOptionalText("Ingrese los apellidos del cliente");

        mostrarDocumentType();
        String documentTypeId = consoleInput.readRequiredText("Ingrese el tipo de documento, ej: 'CC', 'TI'.");
        DocumentType documentType = customerService.getDocumentType(documentTypeId);
        String id = consoleInput.readRequiredText("Ingrese el documento de Identidad del cliente");

        String telephone = consoleInput.readRequiredText("Ingrese el número de telefono del cliente");
        String email = consoleInput.readOptionalText("Ingrese el correo electrónico del cliente");
        String address = consoleInput.readOptionalText("Ingrese la dirección del cliente");

        mostrarCountries();
        short countryId = (short) consoleInput.readInt("Ingrese el Id numérico del pais del cliente", 1, Short.MAX_VALUE);
        Country country = customerService.getCountry(countryId);

        Customer newCustomer = new Customer(id, firsName, lastName, documentType, telephone, country);
        newCustomer.setEmail(email);
        newCustomer.setAddress(address);

        customerService.createCustomer(newCustomer);
        System.out.println("✅ !Cliente registrado con exito¡");
    }

    //Opcion 2. Consultar CLientes
    private void consultarCliente(){
        System.out.println("\n-- CONSULTAR CLIENTE --");
        String id = consoleInput.readRequiredText("Ingrese el documento de identidad del cliente que desea buscar");

        Customer customer =customerService.getCustomer(id);

        System.out.println("\nDatos del cliente:");
        System.out.println("Documento: " + customer.getDocumentType().getId());
        System.out.println("Nombre: " + customer.getFullName());
        System.out.println("País: " + customer.getCountry().getName());
        System.out.println("Telefono: " + customer.getTelephoneNumber());
        System.out.println("Correo: " + (customer.getEmail() != null ?  customer.getEmail() : "Correo no registrado"));
        System.out.println("Dirección: " + (customer.getAddress() != null ? customer.getAddress() : "Dirección no registrada"));
    }

    //Opcion 3. Actualizar Cliente
    private void actualizarContacto(){
        System.out.println("\n-- ACTUALIZAR CONTACTO --");
        String id = consoleInput.readRequiredText("Ingrese el documento del cliente que requiere actualizar datos de contacto");

        Customer existingCustomer = customerService.getCustomer(id);
        System.out.println("Cliente encontrado: " + existingCustomer.getFullName());

        String newTelephoneNumber = consoleInput.readRequiredText("Ingrese el nuevo número de teléfono");
        String newEmail = consoleInput.readOptionalText("Ingrese el nuevo correo electrónico");
        String newAddress = consoleInput.readOptionalText("Ingrese la nueva dirección");

        Customer updates = new Customer(
            existingCustomer.getId(),
            existingCustomer.getFirstName(),
            existingCustomer.getLastName(),
            existingCustomer.getDocumentType(),
            newTelephoneNumber,
            existingCustomer.getCountry()
        );
        updates.setEmail(newEmail);
        updates.setAddress(newAddress);

        customerService.updateCustomer(updates);
        System.out.println("✅ Información de contacto actualizada exitosamente");
    }

    //Opcion 4. Lista de todos los clientes registrados.
    private void listarClientes(){
        System.out.println("\n-- LISTADO DE CLIENTES --");
        List<Customer> customers = customerService.listCustomer();

        if (customers.isEmpty()){
            System.out.println("No hay clientes registrados en el sistema");
            return;
        }
        for (Customer c : customers){
            System.out.println(c.toString() + " | Correo: " + (c.getEmail() != null ? c.getEmail() : "N/A"));
        }
    }

    //Opcion 5. Busqueda por coincidencia de texto
    private void buscarPorApellido(){
        System.out.println("\n-- BUSCAR POR APELLIDO --");
        String lastName = consoleInput.readRequiredText("Ingrese las letras de la coincidencia que desea encontrar");

        List<Customer> results = customerService.searchCustomerByLastName(lastName);

        if (results.isEmpty()){
            System.out.println("No se encontró ninguna coincidencia con: " + lastName + ".");
            for (Customer c : results){
                System.out.println(c.toString());
            }
        }
    }

    //Opcion 6. Eliminar Cliente
    private void eliminarCliente(){
        System.out.println("\n-- ELIMINAR CLIENTE --");
        String id = consoleInput.readRequiredText("Ingrese el numero de documento del cliente que desea eliminar");

        Customer customer = customerService.getCustomer(id);

        boolean seguro = consoleInput.confirm("¿Está seguro que desea eliminar al cliente: " + customer.getFullName() + " ?");

        if (seguro){
            customerService.deleteCustomer(id);
            System.out.println("✅ Cliente eliminado con exito");
        }else {
            System.out.println("Operación cancelada");
        }
    }

    //Metodo para mostrar el tipo de documento
    private void mostrarDocumentType(){
        System.out.println("\nTipos de documento disponibles:");
        for (DocumentType dt : customerService.listDocumentTypes()){
            System.out.println(" - " + dt.getId() + " (" + dt.getDescription() + ")");
        }
    }

    //Metodo para mostrar paises
    private void mostrarCountries(){
        System.out.println("\nPaíses disponibles:");
        for (Country c : customerService.listCountries()){
            System.out.println(" - ID: " + c.getId() + " | " + c.getName());
        }
    }
}
