import amqp from 'amqplib';
import express from 'express';
import bodyParser from 'body-parser';

const app = express();

app.use(express.json());
app.use(bodyParser.json());

app.post('/rabbitmq/send', async (req, res) => {
	const channel = await _connectToRabbitMQ(process.env.RABBITMQ_AMP_URL);
	const message = req.body.message;

	const sendSuccess = await _sendToRabbitMQ(channel, process.env.RABBITMQ_QUEUE, message);

	if (sendSuccess) {
		await res.status(200).send(`Sent message: ${message}`);
	}
	else {
		await res.status(500).send(`Error sending message: ${message}`);
	}
});

const _connectToRabbitMQ = async (ampUrl) => {
	try {
		const connection = await amqp.connect(ampUrl);

		const channel = await connection.createChannel();

		await console.log(`Connected to channel: ${channel.connection.serverProperties.cluster_name}`);

		return await channel;
	} catch (error) {
		console.error('Error connecting to RabbitMQ:', error);

		return error;
	}
};

const _sendToRabbitMQ = async (channel, queue, message) => {
	try {
		await channel.assertQueue(queue, { durable: true });

		return await channel.sendToQueue(queue, Buffer.from(message));
	} catch (error) {
		console.error('Error sending to RabbitMQ:', error);

		return error;
	}
};

app.listen(process.env.APP_PORT, () => {
	console.log(`App listening on ${process.env.APP_PORT}`);
});

export default app;