const submit_btn     = document.querySelector("#regBtn");
const username_input = document.querySelector("#username");
const email_input    = document.querySelector("#email");
const password_input = document.querySelector("#password");
const username_alert = document.querySelector(".username_alert");
const email_alert    = document.querySelector(".email_alert");
const password_alert = document.querySelector(".password_alert");

let em = false;
let us = false
let ps = false;

username_input.addEventListener("input", function (e) {
    if(username_input.value.length < 8){
        username_alert.classList.add("display_block")
        us = false;
    }else{
        username_alert.classList.remove("display_block")
        us = true;
    }
    if(ps && us && em) {
        submit_btn.disabled = false;
    }else{
        submit_btn.disabled = true;
    }
});


email_input.addEventListener("input", function (e) {
    if(!email_input.value.includes("@mail.ru") &&  !email_input.value.includes("@gmail.com")){
        email_alert.classList.add("display_block")
        em = false;
    }else{
        email_alert.classList.remove("display_block")
        em = true;
    }
    if(ps && us && em) {
        submit_btn.disabled = false;
    }else{
        submit_btn.disabled = true;
    }
});

password_input.addEventListener("input", function (e) {
    if(password_input.value.length < 8){
        password_alert.classList.add("display_block")
        ps = false;
    }else{
        password_alert.classList.remove("display_block")
        ps = true;
    }
    if(ps && us && em) {
        submit_btn.disabled = false;
    }else{
        submit_btn.disabled = true;
    }
});

