const theme_changer = document.querySelector("#them_btn")
theme_changer.addEventListener("click", function (){
    if(document.body.classList.contains("light-theme")){
        document.body.classList.remove("light-theme")
        theme = 1;
    }else{
        document.body.classList.add("light-theme")
        theme = 2;
    }
})