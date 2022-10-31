import java.nio.*;
/*
*一, 缓冲区(Buffer)的概念:
     缓冲区在NIO中负责存储数据，相当于火车车厢，然后在一种类似现实中铁轨的上来回传送数据，与IO单向水管式的传送有区别；

*二, 根据数据类型不同提供不同的缓冲区类型(没有boolean的buffer)
     ByteBuffer
	 CharBuffer
	 ShotBuffer
	 IntBuffer
	 LongBuffer
	 FloatBuffer
	 DoublerBuffer
	 这几种Buffer缓冲区的管理方式一样，首先调用allocate(..)方法创建一块缓冲区 
*三, 缓冲区类有两个主要方法
	 put(): 向缓冲区存数据
	 get(): 获取缓冲区的数据
*四, 缓冲区的四个核心属性
	 capacity : 缓冲区的容量，一旦规定不可更改；
	 limit : 界限，表示缓冲区中最多可操作数据的多少，limit后面的数据不能读写，不是有效数据，可能是之前内存中未初始化的旧数据
	 position : 位置，指缓冲区中正在操作的数据的位置
	 mark : 标记，表示记录当前position的位置。可通过reset()方法恢复到mark的位置
	 大小关系
	 0 <= mark <= position <= limit <= capacity
*五, 非直接缓冲区和直接缓冲区
	 (1)非直接缓冲区，指由JVM在其分配一块内存来给NIO当缓冲区用，
	    优点是可直接操作，这块内存可控
		缺点是速度慢，因为这块内存的数据还要复制到系统管理的内存，再传到硬盘里；
		使用 allocate(..)方法
	 (2)直接缓冲区，指直接调用系统的一块内存作为缓冲区
	    优点：速度快
		缺点：不可控，NIO程序把数据传到这块内存后，什么时候存到硬盘就有系统决定了。
		使用 allocateDirect(..)方法

*/


public class ByteBufferTest {

	public static void main(String[] args){
		
		testBuffer();
		System.out.println("分割线=====================");
		testBuffer02();
		System.out.println("分割线,测试直接缓冲区=====================");
		testAllocateDirect();
	}

	public static void testBuffer(){
		String str = "abcdefg";
		//使用缓存区的步骤
		//1,首先创建一块缓存区，这里的缓存时是非直接缓存区，在jvm中
		ByteBuffer buf = ByteBuffer.allocate(1024);  //1024 bytes
		System.out.println("***********allocate(1024)*********");
		System.out.println("position==>" + buf.position());  //结果：0，下标从0开始
		System.out.println("limit==>" + buf.limit());        //结果：1024，  limit()方法
		System.out.println("capacity==>" + buf.capacity());  //结果：1024，容量
		
		//2, 把String类型数据放到缓存, 需要将其转换成最小单位byte类型
		System.out.println("**********put(..)**********");
		buf.put(str.getBytes());
		System.out.println("position==>" + buf.position());  //结果：7，下标从0开始，position箭头指在将要读写的位置，即"g"后面的位置，
		System.out.println("limit==>" + buf.limit());        //结果：1024，
		System.out.println("capacity==>" + buf.capacity());  //结果：1024，容量
	
	    //3, 调用flip()方法，切换到读取数据的模式,对应的position和limit的箭头位置要变化，上面是写数据模式
		System.out.println("**********flip()**********");
		buf.flip();
		System.out.println("position==>" + buf.position());  //结果：0, 即调用flip()以后，把position箭头指向了第一个数据，准备读取
		System.out.println("limit==>" + buf.limit());        //结果：1024，
		System.out.println("capacity==>" + buf.capacity());  //结果：1024，容量

		//4, 调用get(..)方法读取数据
		byte[] bytes = new byte[buf.limit()];  //初始化一个数组，设置数组的容量和缓存的相等
		buf.get(bytes);   //读取数据放到这个数组中
		System.out.println("**********get(..)**********");
		System.out.println("bytes==>" + new String(bytes,0,buf.limit()));  //把数组转换为字符串，结果"abcdefg"
		System.out.println("position==>" + buf.position());  //结果：7, 说明读完数据后position的箭头指向了下标7，
		System.out.println("limit==>" + buf.limit());        //结果：7，limit的箭头也指向了下标7
		System.out.println("capacity==>" + buf.capacity());  //结果：1024，容量

		//5, 调用rewind()方法，使其可重复读，即把positon的箭头指向了下标0
		System.out.println("**********rewind(..)**********");
		buf.rewind();
		System.out.println("position==>" + buf.position());  //结果：0,position箭头指向0，又可以再次读取数据了；
		System.out.println("limit==>" + buf.limit());        //结果：7，limit的箭头也指向了下标7
		System.out.println("capacity==>" + buf.capacity());  //结果：1024，容量

		//6, 调用clear()方法，清空缓存区，实际还有数据，只是不能再读取了
		System.out.println("**********clear()**********");
		buf.clear();
		System.out.println("position==>" + buf.position());  //结果：0,position箭头指向0
		System.out.println("limit==>" + buf.limit());        //结果：1024, limit箭头指向最大容量，表示缓存中数据清空了
		System.out.println("capacity==>" + buf.capacity());  //结果：1024，容量

		//7, 获取单个字节，虽然上面清空了，但是这里还可获取数据,只是position,limit的箭头都初始化了。
		for(int i=0; i < 7; i++){
			System.out.println("get char==>" + (char)buf.get());
		}
		
	}


	public static void testBuffer02(){
		/*8，mark()方法的用法:
		  标记当前positon箭头指向的位置，后面调用reset()方法会恢复到此位置
		*/
		String  str = "abcde";
		ByteBuffer buffer = ByteBuffer.allocate(1024);

		buffer.put(str.getBytes());
		byte[] bytes = new byte[buffer.limit()];

		//注意转换为读缓存模式一定记得调用flip()方法
		buffer.flip();

		buffer.get(bytes,0,2);     //获取缓存区指定下标的部分字节,0是起始下标，2是长度，存到bytes数组里面
		System.out.println("bytes==>" + new String(bytes,0,buffer.limit()));
		
		//调用mark()方法，标记当前position位置
		buffer.mark();    
		System.out.println("position mark==>" + buffer.position());
		//继续读取数据
		buffer.get(bytes,2,3);
		System.out.println("bytes02==>" + new String(bytes,2,3));  //输出“cde”
		System.out.println("position now==>" + buffer.position());
		
		//reset()方法执行，使position回到mark标记的位置
		buffer.reset();
		System.out.println("position after reset==>" + buffer.position());
		
		//9, remaining()用法，查看缓冲区还有多少可操作的数据
		int remainingNum = buffer.remaining();
		System.out.println("remaining==>" + remainingNum);   //输出：3，因为上面reset()方法执行了，position在2的位置

	}

	//直接缓冲区范例
	public static void testAllocateDirect(){
		ByteBuffer directBuffer = ByteBuffer.allocateDirect(1024);  //分配直接缓冲区
		String abc = "abcdefgh";
		System.out.println("isDirect==>" + directBuffer.isDirect());
		directBuffer.put(abc.getBytes());
		System.out.println("directBuffer's postion==>" + directBuffer.position());
		//调用flip()方法，准备读取数据到数组里
		directBuffer.flip();
		byte[] bytes = new byte[directBuffer.limit()];  
		directBuffer.get(bytes);
		System.out.println("bytes==>" + new String(bytes));
		
	}
}