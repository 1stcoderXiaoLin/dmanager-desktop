import { createApp } from "vue";
import {
  create,
  NButton,
  NCard,
  NDataTable,
  NEmpty,
  NForm,
  NFormItem,
  NInput,
  NInputNumber,
  NModal,
  NPopconfirm,
  NScrollbar,
  NSelect,
  NSpace,
  NTag,
} from "naive-ui";
import App from "./App.vue";
import "./styles/base.css";

const naive = create({
  components: [
    NButton,
    NCard,
    NDataTable,
    NEmpty,
    NForm,
    NFormItem,
    NInput,
    NInputNumber,
    NModal,
    NPopconfirm,
    NScrollbar,
    NSelect,
    NSpace,
    NTag,
  ],
});

const app = createApp(App);
app.use(naive);
app.mount("#app");
