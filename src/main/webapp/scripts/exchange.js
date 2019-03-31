function currencyToEmojiFlagHtml(currName) {
    return "<i class=\"em em-flag-" + currName.toLowerCase().slice(0, -1) + "\"></i>";
}

function createHeader(input) {
    switch (input.type) {
        case "MULTI_CURRENCY_EXPR":
        case "SINGLE_CURRENCY_EXPR":
            return "<i class=\"em em-hash\"></i> <i>" + input.expression + " =</i>\n";
        case "SINGLE_VALUE":
            if (input.expression === "1") {
                return "<i class=\"em em-chart\"></i> <i>Курс " + input.involved_currencies[0] + "</i>\n";
            }
        default:
            return "";
    }
}

function humanize(value, precision) {
        value = value || "";
        precision = precision || 0;

        var rounded, trimmed;

        rounded = (!isNaN(precision) && parseInt(precision, 10) > 0)
            ? parseFloat(value).toFixed(parseInt(precision, 10))
            : value;

        trimmed = parseFloat(rounded).toString();

        return trimmed;
};


$( document ).ready(function() {
    $("#sendQuery").on("click", function() {
        var query = $( "#query" ).val();
        $.ajax({
          url  : '/exchange',
          type : 'post',
          data : '' + query
        }).done(function(data, statusText, xhr) {
          var res = createHeader(data.input);
          data.rates.forEach(function(e) {
            res = res + currencyToEmojiFlagHtml(e.currency.code) + " <b>" + e.currency.code + "</b> " + humanize(e.sum, 3) + "\n";
          });
          $("#result").html(res);
        }).fail(function(data, statusText, xhr){
          var e = data.responseJSON;
          $("#result").html(e.message + " (at " + e.error_position + ")\n"
                          + "▼".padStart((e.error_position > e.raw_input.length ? e.raw_input.length : e.error_position) + 2) + "\n"
                          + "> " + e.raw_input + "\n"
                          + "▲".padStart((e.error_position > e.raw_input.length ? e.raw_input.length : e.error_position) + 2))
        });
    })

    $("#sendQuery").click();
})
