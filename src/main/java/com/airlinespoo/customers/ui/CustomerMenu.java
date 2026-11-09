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
            System.out.println("1. Registrar Cliente");
            System.out.println("2. Consultar cliente por documento");
            System.out.println("3. Actualizar contacto de cliente");
            System.out.println("4. Listar todos los clientes");
            System.out.println("5. Buscar clientes por apellido");
            System.out.println("6. Eliminar cliente");
            System.out.println("7. Registrar nuevo País");
            System.out.println("8. Listar Países");
            System.out.println("9. Listar Tipos de Documento");
            System.out.println("10. Registrar nuevo tipo de Documento");
            System.out.println("11. Ver total de clientes registrados");
            System.out.println("12. Buscar clientes por País");
            System.out.println("13. Buscar clientes por Tipo de Documento");
            System.out.println("14. Salir");

            // Aumentamos el límite de opciones a 10
            int option = consoleInput.readInt("Selecciona una opción: ", 1, 14);

            try {
                switch (option){
                    case 1 -> registrarCliente();
                    case 2 -> consultarCliente();
                    case 3 -> actualizarContacto();
                    case 4 -> listarClientes();
                    case 5 -> buscarPorApellido();
                    case 6 -> eliminarCliente();
                    case 7 -> registrarPais();
                    case 8 -> mostrarCountries();
                    case 9 -> mostrarDocumentType();
                    case 10 -> registrarTipoDocumento();
                    case 11 -> countCustomers();
                    case 12 -> listarPorPais();
                    case 13 -> listarPorTipoDocumento();
                    case 14 -> salir = true;
                }
            } catch (AirlinesException exception){
                System.out.println("❌ Error  " + exception.getMessage());
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
        DocumentType documentType = customerService.getDocumentType(documentTypeId.toUpperCase());
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
        } else {
            System.out.println("Resultados encontrados:");
            for (Customer c : results){
                System.out.println(c.toString() + " | Correo: " + (c.getEmail() != null ? c.getEmail() : "N/A"));
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

    //Opcion 7. Registrar un nuevo país en el catálogo
    private void registrarPais(){
        System.out.println("\n-- REGISTRAR NUEVO PAÍS --");

        // El ID numérico suele requerirse mayor a 0
        short id = (short) consoleInput.readInt("Ingrese el código numérico del nuevo país", 1, Short.MAX_VALUE);
        String name = consoleInput.readRequiredText("Ingrese el nombre del país");

        Country newCountry = new Country(id, name);

        customerService.createCountry(newCountry);
        System.out.println("✅ País registrado con éxito.");
    }

    //Opcion 8. Listar Paises
    private void mostrarCountries(){
        System.out.println("\nPaíses disponibles:");
        for (Country c : customerService.listCountries()){
            System.out.println(" - ID: " + c.getId() + " | " + c.getName());
        }
    }

    //Opcion 9. Metodo para mostrar el tipo de documento
    private void mostrarDocumentType(){
        System.out.println("\nTipos de documento disponibles:");
        for (DocumentType dt : customerService.listDocumentTypes()){
            System.out.println(" - " + dt.getId() + " (" + dt.getDescription() + ")");
        }
    }

    // Opcion 10. Registrar un nuevo tipo de documento
    private void registrarTipoDocumento(){
        System.out.println("\n-- REGISTRAR NUEVO TIPO DE DOCUMENTO --");

        String id = consoleInput.readRequiredText("Ingrese el código del tipo de documento (exactamente 2 letras, ej: 'CC', 'TI')");
        String description = consoleInput.readRequiredText("Ingrese la descripción (ej: 'Cédula de Ciudadanía')");

        // Pasamos el ID a mayúsculas para asegurar la regla de normalización desde la captura
        DocumentType newDocType = new DocumentType(id.toUpperCase(), description);

        customerService.createDocumentType(newDocType);
        System.out.println("✅ Tipo de documento registrado con éxito.");
    }

    // Opcion 11.: Mostrar total de clientes
    private void countCustomers(){
        long total = customerService.countCustomers();
        System.out.println("\n📊 Total de clientes registrados en el sistema: " + total);
    }

    // Opcion 12: Listar clientes por un país específico
    private void listarPorPais(){
        System.out.println("\n-- CLIENTES POR PAÍS --");
        mostrarCountries();
        short countryId = (short) consoleInput.readInt("Ingrese el Id numérico del país a consultar", 1, Short.MAX_VALUE);

        // Asumiendo que codificaste listCustomersByCountry en tu servicio
        List<Customer> byCountry = customerService.listCustomerByCountry(countryId);

        if (byCountry.isEmpty()){
            System.out.println("No hay clientes registrados en este país.");
        } else {
            for (Customer c : byCountry){
                System.out.println(c.toString());
            }
        }
    }

    // Opcion 13: Listar clientes por tipo de documento
    private void listarPorTipoDocumento(){
        System.out.println("\n-- CLIENTES POR TIPO DE DOCUMENTO --");
        mostrarDocumentType();
        String docType = consoleInput.readRequiredText("Ingrese el código del tipo de documento (ej: CC)");

        // Asumiendo que codificaste listCustomersByDocumentType en tu servicio
        List<Customer> porTipo = customerService.listCustomerByDocumentType(docType.toUpperCase());

        if (porTipo.isEmpty()){
            System.out.println("No hay clientes registrados con este tipo de documento.");
        } else {
            for (Customer c : porTipo){
                System.out.println(c.toString());
            }
        }
    }
}
