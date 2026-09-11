# MongoDB Atlas: configuracion del examen

Se requiere un cluster Atlas Free (M0), sin MongoDB local ni contenedores.
La provision real requiere acceso a una cuenta Atlas; este archivo no implica que se haya creado.

1. Crea un proyecto aislado llamado tvmaze-examen.
2. Crea un cluster en el tier **Free / M0**. No selecciones Flex ni un tier de pago.
3. En Network Access, agrega **0.0.0.0/0** para cumplir el requisito de acceso sin restriccion
   de IP. La autenticacion y TLS permanecen habilitados. Este ajuste es para el examen:
   permite conexiones desde cualquier IPv4 y no debe reutilizarse en produccion.
4. Crea un usuario de base de datos con readWrite sobre tvmaze.
5. En Connect > Drivers, copia la cadena mongodb+srv. Configura MONGODB_URI en tu entorno
   y MONGODB_DATABASE=tvmaze. Usa percent-encoding en los caracteres especiales del password.
6. Arranca la aplicacion y consulta /actuator/health.

La coleccion show_cache usa _id=show_id y guarda payload (JSON original) y cachedAt.
No existe TTL: el examen exige devolver el objeto cuando el ID ya esta registrado.
Los errores de lectura o escritura se traducen a 503; un miss solo responde despues de persistir.
