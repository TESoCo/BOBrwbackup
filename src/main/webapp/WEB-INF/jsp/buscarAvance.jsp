<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>Buscar Avance</title>
</head>
<body>
    <h2>Buscar en Web Service</h2>
    <form method="post">
        <input type="text" name="criterio" placeholder="Criterio de búsqueda" required>
        <button type="submit">Buscar</button>
    </form>

    <c:if test="${not empty resultado}">
        <h3>Resultado: ${resultado}</h3>
    </c:if>

    <c:if test="${not empty error}">
        <h3 style="color: red;">Error: ${error}</h3>
    </c:if>
</body>
</html>