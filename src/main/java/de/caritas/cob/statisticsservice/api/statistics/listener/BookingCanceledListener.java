package de.caritas.cob.statisticsservice.api.statistics.listener;

import de.caritas.cob.statisticsservice.api.model.BookingCanceledStatisticsEventMessage;
import de.caritas.cob.statisticsservice.api.model.BookingRescheduledStatisticsEventMessage;
import de.caritas.cob.statisticsservice.api.statistics.model.statisticsevent.StatisticsEvent;
import de.caritas.cob.statisticsservice.api.statistics.model.statisticsevent.meta.BookingCanceledMetaData;
import de.caritas.cob.statisticsservice.api.statistics.model.statisticsevent.meta.BookingRescheduledMetaData;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

/** AMQP Listener for create message statistics event. */
@Service
@RequiredArgsConstructor
public class BookingCanceledListener {

  private final @NonNull MongoTemplate mongoTemplate;

  /**
   * Consumer for create message statics statistics event.
   *
   * @param eventMessage the {@link BookingCanceledStatisticsEventMessage} instance
   */
  @RabbitListener(
      id = "booking-canceled-event-listener",
      queues = "#{rabbitMqConfig.QUEUE_NAME_BOOKING_CANCELED}",
      containerFactory = "simpleRabbitListenerContainerFactory")
  public void receiveMessage(BookingCanceledStatisticsEventMessage eventMessage) {

    StatisticsEvent statisticsEvent = new StatisticsEvent();

    mongoTemplate.insert(statisticsEvent);
  }

  private BookingCanceledMetaData buildMetaData(BookingCanceledStatisticsEventMessage eventMessage) {
    return BookingCanceledMetaData.builder().build();
  }
}