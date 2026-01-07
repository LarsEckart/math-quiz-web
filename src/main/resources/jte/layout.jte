@import gg.jte.Content

@param String title
@param Content content

<!DOCTYPE html>
<html lang="et">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${title}</title>
    <link rel="stylesheet" href="/app.css">
    <script src="https://unpkg.com/htmx.org@1.9.10"></script>
</head>
<body>
    <main>
        ${content}
    </main>
</body>
</html>
