package com.canyue.mqtt.ui.controller;

import com.canyue.mqtt.core.Message;
import com.canyue.mqtt.core.client.impl.MqttClient;
import com.canyue.mqtt.core.event.ClientStatusEvent;
import com.canyue.mqtt.core.event.MessageEvent;
import com.canyue.mqtt.core.exception.MqttException;
import com.canyue.mqtt.core.exception.MqttIllegalArgumentException;
import com.canyue.mqtt.core.listener.ClientStatusListener;
import com.canyue.mqtt.core.listener.MessageReceivedListener;
import com.canyue.mqtt.core.persistence.impl.FilePersistence;
import com.canyue.mqtt.ui.config.ConnConfig;
import com.canyue.mqtt.ui.data.DataHolder;
import com.canyue.mqtt.ui.data.TopicFilterData;
import com.canyue.mqtt.ui.utils.TopicFilterMatcher;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Modality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author canyue
 */
public class ConnController  {
    @FXML
    private Label lbClientId;
    @FXML
    private Button btnConnect;
    @FXML
    private Button btnDisconnect;

    private static Logger logger = LoggerFactory.getLogger(ConnController.class);
    private ClientController clientController;
    private DataHolder dataHolder;
    private ListView<Message> lvMessage;
    private TabPane tabPane;
    private ListView<TopicFilterData> lvTopicFilter;
    private Map<String, TopicFilterData> map;

    public void connect(ActionEvent actionEvent) {
        logger.debug("正在连接建立");
        try {
            ConnConfig connConfig = dataHolder.getConnConfig();
            MqttClient client = MqttClient.getBuilder()
                    .setHost(connConfig.getHost())
                    .setPort(connConfig.getPort())
                    .setMessageReceivedListener(new MyMessageReceivedListener())
                    .setClientStatusListener(new MyClientStatusListener())
                    .setPersistence(new FilePersistence())
                    .build();
            dataHolder.setMqttClient(client);
            client.start();
            client.connect(connConfig.getUsername(),connConfig.getPassword(),connConfig.getClientId(),null,connConfig.getKeepAlive(),connConfig.isCleanSession());
            logger.debug("连接已建立");
            this.dataHolder.setRunStatus(true);
        } catch (MqttException e) {
            logger.error("连接失败:", e);
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.initModality(Modality.NONE);
                    alert.setTitle("连接失败！");
                    alert.setHeaderText("");
                    alert.setContentText("请检查：\n1.服务器是否开启\n2.您的网络状态是否正常");
                    alert.show();
                }
            });
        }
    }

    public void disconnect(ActionEvent actionEvent) {
        try {
            logger.debug("正在断开连接");
            dataHolder.getMqttClient().disconnect();
            btnDisconnect.setDisable(true);
            btnConnect.setDisable(false);
            tabPane.setDisable(true);
            lvMessage.getItems().removeAll();
            dataHolder.setRunStatus(false);
            logger.info("连接已断开！");
        } catch (MqttException e) {
            logger.error("断开连接失败：",e);
        }
    }


    public void injectMainController(ClientController clientController) {
        System.out.println("ConnController.injectMainController");
        this.clientController = clientController;
        init();
    }

    private void init() {
        this.lvMessage = this.clientController.getLvMessage();
        this.lvTopicFilter = this.clientController.getLvTopicFilter();
        this.tabPane = this.clientController.getTabPane();
        dataHolder = clientController.getDataHolder();
        lbClientId.setText(dataHolder.getConnConfig().getClientId());
    }

    @FXML
    private void initialize(){
    }

    class MyMessageReceivedListener implements MessageReceivedListener {
        @Override
        public void messageArrived(MessageEvent messageEvent) {
            Message message = messageEvent.getMessage();
            if (message != null) {
                Set<String> topicFiltersSet = map.keySet();
                for (String topicFilter : topicFiltersSet) {
                    try {
                        boolean flag = TopicFilterMatcher.match(topicFilter, message.getTopic());
                        if (flag) {
                            Platform.runLater(new Runnable() {
                                @Override
                                public void run() {
                                    map.get(topicFilter).getMessageObservableList().add(message);
                                }
                            });
                        }
                    } catch (MqttIllegalArgumentException e) {
                        e.printStackTrace();
                    }
                }
            }

        }
    }
    class MyClientStatusListener implements ClientStatusListener {
        @Override
        public void connectCompeted(ClientStatusEvent clientStatusEvent) {
            btnConnect.setDisable(true);
            btnDisconnect.setDisable(false);
            tabPane.setDisable(false);
        }

        @Override
        public void shutdown(ClientStatusEvent clientStatusEvent) {
            dataHolder.setRunStatus(false);
            btnDisconnect.setDisable(true);
            btnConnect.setDisable(false);
            tabPane.setDisable(true);
            logger.info("连接已断开！");
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.initModality(Modality.NONE);
                    alert.setTitle("连接已断开！");
                    alert.setHeaderText("");
                    alert.setContentText("连接已断开！");
                    alert.show();
                }
            });
        }

        @Override
        public void subscribeCompeted(ClientStatusEvent clientStatusEvent) {
            String[] topicFilters = clientStatusEvent.getTopicFilters();
            if (topicFilters != null) {
                System.out.println("订阅成功:" + Arrays.toString(topicFilters));
                if (map == null) {
                    map = new HashMap<>();
                }
                if (!map.containsKey(topicFilters[0])) {
                    TopicFilterData tfd = new TopicFilterData(topicFilters[0], FXCollections.observableArrayList());
                    map.put(topicFilters[0], tfd);
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            dataHolder.getFilterDataList().add(tfd);
                        }
                    });
                } else {
                    System.out.println("你已经订阅过" + topicFilters[0]);
                }
            }
        }

        @Override
        public void unsubscribeCompeted(ClientStatusEvent clientStatusEvent) {
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    String[] topicFilters = clientStatusEvent.getTopicFilters();
                    if (topicFilters != null) {
                        System.out.println("取消订阅成功" + Arrays.toString(topicFilters));
                        dataHolder.getFilterDataList().remove(map.get(topicFilters[0]));
                        map.remove(topicFilters[0]);
                    }
                }
            });

        }
    }
}


