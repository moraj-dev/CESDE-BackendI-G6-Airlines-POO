## Módulo que entrego

<!-- Modulo : Seat y DetailsReservations -->

## Qué clases y piezas incluye

<!-- Lista las clases nuevas: modelo, repositorio, servicio, menú, datos de ejemplo -->

- **Modelo**
  - `Seat` — Representa una silla mediante su fila y número. Valida ambos datos y define la igualdad a partir de la fila y el número.
  - `ReservationDetail` — Representa el detalle de un pasajero dentro de una reserva. Guarda el ID, el nombre completo, el documento y la silla asignada. Valida esos datos y define la igualdad a partir del ID.


## Reglas que validé

<!-- Resume las reglas de la guía que implementaste -->

1. Seat: Las reglas que implemente fueron :

rownumber :
Es obligatoria. Se rechazan null, el texto vacío y las filas con solo espacios.
Se eliminan los espacios de los extremos y se convierte a mayúsculas antes de validar y guardar.
Debe tener uno o dos caracteres. Puede contener letras de A a Z, dígitos de 0 a 9 o una combinación.

seatNumber:
Debe ser un número entero entre 1 y 77, incluidos ambos límites.
Se aceptan 1, 25 y 77. Se rechazan 0, -1 y 78.

2. DetailsReservation :

ID del detalle (id)
- Debe ser un entero positivo. El mínimo es 1.
- Se recibe como parámetro del constructor.
- El ID define la igualdad. Si dos detalles tienen el mismo id, equals() devuelve true aunque el nombre, el documento o la silla sean distintos. hashCode() también se calcula solo con el id.
Nombre completo del pasajero (passengerFullName)
- Es obligatorio. Se rechazan null, el texto vacío y el texto con solo espacios.
- Se eliminan los espacios de los extremos antes de validar la longitud y guardar el nombre.
- Admite hasta 50 caracteres. Los espacios internos, las tildes y los guiones están permitidos. Por ejemplo, "  María José  " se guarda como "María José".
Documento del pasajero (passengerDocument)
- Es obligatorio. Se rechazan null, el texto vacío y el texto con solo espacios.
- Se eliminan los espacios de los extremos antes de validar y guardar el documento.
- Admite hasta 20 caracteres. Solo permite letras de A a Z y de a a z, y dígitos de 0 a 9. Por ejemplo, " Ab123 " se guarda como "Ab123", mientras que "AB-123" se rechaza.
Silla del pasajero (seat)
- Es obligatoria. El constructor debe recibir un objeto Seat distinto de null.
- La clase Seat se encarga de validar la fila y el número de la silla cuando se crea ese objeto.
Cuando se incumple alguna validación, el constructor lanza IllegalArgumentException. Los cuatro atributos son private final, así que se asignan al crear el detalle y luego se consultan mediante sus getters.



-

## Casos de prueba superados

<!-- Marca los casos de la tabla de tu módulo que ejecutaste a mano -->

## Casos de prueba superados

### Seat

- [*] Creé una silla con la fila `" a1 "` y comprobé que se guardó como `"A1"`.
- [*] Comprobé que se aceptan los números de silla `1` y `77`.
- [*] Comprobé que se rechazan los números de silla `0` y `78`.
- [*] Comprobé que se rechazan las filas `null`, `"   "`, `"ABC"` y `"A 1"`.
- [*] Verifiqué que dos sillas con la misma fila y número son iguales y tienen el mismo `hashCode()`.

### ReservationDetail

- [*] Creé un detalle con ID positivo, nombre, documento y silla válidos.
- [*] Comprobé que se rechaza un detalle con ID `0`.
- [*] Comprobé que se rechaza un nombre vacío o de más de 50 caracteres.
- [*] Comprobé que se rechaza un documento vacío, con guion o de más de 20 caracteres.
- [*] Comprobé que se rechaza un detalle con silla `null`.
- [*] Verifiqué que dos detalles con el mismo ID son iguales y tienen el mismo `hashCode()`.

- [*] Ejecuté todos los casos de prueba de la tabla de mi módulo.

## Dudas o decisiones que quiero comentar

<!-- La verdad primeramente estas dos tablas son importantes en reservations que leda la base a este moudo-->

## Checklist antes de pedir revisión

- [*] La rama es `feature/tblDetailsReservaion` y nació de `develop/erikM`
- [*] Este PR apunta a `develop` (no a `main`)
- [*] El proyecto compila y el programa se ejecuta
- [*] `git status` solo muestra archivos de mi módulo, sin `target/` ni `.idea/`
- [*] Los commits siguen el formato `tipo: descripción`
- [ ] Traje lo último de `develop` antes de abrir este PR
- [ ] Marqué como revisor a un compañero y al docente
- [ ] Mi bitácora está al día y puedo explicar mi código en voz alta
