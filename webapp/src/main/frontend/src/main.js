import es from "./common/i18n/messages_es.properties?raw";
import en from "./common/i18n/messages_en.properties?raw";
import { configureMessages } from "./common/i18n/I18n.js";
import { createApp } from "vue";
import "bootstrap/dist/css/bootstrap.min.css";
import "./common/styles/app.css";
import App from "./App.vue";
configureMessages({ es, en });
createApp(App).mount("#app");
