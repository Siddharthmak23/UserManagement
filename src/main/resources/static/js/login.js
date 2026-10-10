(function ($) {
    "use strict";

    $("#togglePassword").on("click", function () {
        var input = $("#password");
        var showing = input.attr("type") === "text";
        input.attr("type", showing ? "password" : "text");
        $(this).text(showing ? "Show" : "Hide");
    });

    $("#loginForm").on("submit", function () {
        if (this.checkValidity()) {
            $("#loginButton").prop("disabled", true);
        }
    });
})(jQuery);
