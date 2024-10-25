package semiLA.layoutDesigner.analysis.Micro;

//import java.sql.Date;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Date;
import java.sql.Timestamp;

import semiLA.data.device.EQ;

//import semiLA.layoutDesigner.TestData;
import semiLA.io.file.LogFile;
import semiLA.layoutDesigner.CDataSet;
import semiLA.layoutDesigner.CResultSet;
import semiLA.layoutDesigner.Area.CBay;
import semiLA.layoutDesigner.Area.CZone;
//import semiLA.layoutDesigner.EQ.CEQ;
import semiLA.layoutDesigner.EQ.CModule;
import semiLA.layoutDesigner.EQ.CRoom;
import semiLA.layoutDesigner.EQ.CSubRoom;

public class MicroGenetic
{
	CDataSet dataSet = CDataSet.getInstance();
	CResultSet resultSet = CResultSet.getInstance();

	/** Room , 공정 군을 의미 */
	private CRoom room;
	int population_number = 100;
	int max_iteration = 2;
	
	public int accumulated_value = 0;
	public double current_best_fitness = 1000000;
	
//	GA가 몇번 동안 몇 % 이내로 성능개선이 없으면 그만 돌건지
	int stationary_number = 4;
	double stationary_slack = 0.001;
	
//	공정 개수를 현재는 11개로 fix했는데 다른곳에서 받아오는식으로 수정할 필요는있음
	int facil_number =11;
	
	private ArrayList<Renewal_MicroOrganism> n_lstPopulation = new ArrayList<Renewal_MicroOrganism>();
	private ArrayList<Double> lst_roulette;


	
	
	/** Module을 Integer로 Mapping */
	private Map<Integer, CModule> propertiesByInt;
	private Map<String, Integer> propertiesByName;

	/** Module을 관리하는 리스트 */
	private ArrayList<CModule> moduleList;

	/** 인구(Population, 생물체의 모임)을 나타내는 변수 */
	private ArrayList<MicroOrganism> lstPopulations;

	/** 유전자 알고리즘 계산에서 사용하는 인구(Population, 생물체의 모임) 변수의 버퍼 */
	private ArrayList<MicroOrganism> buffer;

	/** 현재 fitness function 값*/
	private double pastFitnessFunctionValue;
	private double currentFitnessFunctionValue;
	private boolean isBetterSolution;
	
	/** 모듈별 설비 수 */
	private int[] moduleEqNum;

	
	
	/**
	 * 생성자
	 */
	public MicroGenetic()
	{
	}

	/**
	 * 
	 * @author : kyveri.kim
	 * @version : 2010. 07. 13 오전 9:54:34
	 * @param logFile 
	 * @date : 2010. 07. 13
	 * @변경이력 :
	 * @Method 설명 : GA를 Run 하는 Method
	 */
	
	
	public Renewal_MicroOrganism start()
	{
		System.out.println("GA 시작");
		gen_population();		
		
		calfitness_and_set(n_lstPopulation);

		
//		일단은 제일 좋은놈 하나 넣어두자
		Renewal_MicroOrganism current_solution = new Renewal_MicroOrganism();
		
		int accumulated_value = 0;
		
		for(int i=0; i<max_iteration; i++) {
			System.out.println(i+"generation 시작");
			// 현재 generation의 fitness 출력
			System.out.println(i+" generation의 fitness"+elitism().fitness_value);
			
			ArrayList<Renewal_MicroOrganism> temp_population = new ArrayList<Renewal_MicroOrganism>();
//			제일 좋은놈 하나는 일단 추가
			temp_population.add(elitism());
			
			while(temp_population.size() < population_number) {
				
//				중복되는 애들이 많이 보이는것 같으면 중복 제거 코드도 추가해야합니다
				Renewal_MicroOrganism selected_one = selection();
				Renewal_MicroOrganism selected_two = selection();
				
				Renewal_MicroOrganism[] temp_result = crossover(selected_one,selected_two);
//				
//				System.out.println("바뀐후");
//				for (i = 0; i < selected_one.each_chromosome.size(); i++)
//				{
//				System.out.print(selected_one.each_chromosome.get(i).getM_strName()+" ");
//				}
//				System.out.println("");
//				System.out.println("한놈 확인");
//				for (i = 0; i < temp_result[0].each_chromosome.size(); i++)
//				{
//				System.out.print(temp_result[0].each_chromosome.get(i).getM_strName()+" ");
//				}
//				System.out.println("");
//				for (i = 0; i < temp_result[1].each_chromosome.size(); i++)
//				{
//				System.out.print(temp_result[1].each_chromosome.get(i).getM_strName()+" ");
//				}
				
				temp_population.add(temp_result[0]);
				temp_population.add(temp_result[1]);
			}
			
			
			//System.out.println("잘 뽑혔는지 확인해보자");
			temp_population = mutation(temp_population);
			
//			------------------------Fitness 싹다 계산하는게 필요하다------------------------
//			temp_population = cal_fitness(temp_population);
			
			Date date3 =new Date();
			System.out.println(new Timestamp(date3.getTime()));
			
			calfitness_and_set(temp_population);
			
			Date date4 =new Date();
			System.out.println(new Timestamp(date4.getTime()));
			
			
//			cast하는게 맞는지는 모르겠음
			n_lstPopulation = (ArrayList<Renewal_MicroOrganism>) temp_population.clone();
			
//			최적해 찾은것 같으면 break		
			if(check_stop_criteria(elitism())) {
				//break;
			}
		}
		
		return elitism();
	}
	
	
//	멈춰도 될지를 체크하는 함수
	public boolean check_stop_criteria(Renewal_MicroOrganism check_this)
	{
		if(check_this.fitness_value < current_best_fitness * (1 + stationary_slack) && accumulated_value > stationary_number) {
	
			return true;	
		}else if(check_this.fitness_value < current_best_fitness * (1 + stationary_slack)){
			current_best_fitness = check_this.fitness_value;
			accumulated_value+=1;
			return false;	
		}else {
			accumulated_value = 0;
			return false;	
		}
	}
	
	
//	가장 좋은 해 하나는 데리고 다니기
	public Renewal_MicroOrganism elitism(){
		
		double best_solution = 1000000000000000000000000000000000.00;
		int best_index = -1;
//		fitness가 큰게 좋은지 작은지 좋은지에 따라 달라지기는 하지만 우선은 작은게 좋다고 함
		for(int i=0;i<n_lstPopulation.size();i++) {
			if(n_lstPopulation.get(i).fitness_value < best_solution) {
				best_solution = n_lstPopulation.get(i).fitness_value;
				best_index = i;
			}
		}
		return n_lstPopulation.get(best_index);
	}
	
	public void calfitness_and_set(ArrayList<Renewal_MicroOrganism> input_array){
//		fitness 계산하고 배치된 결과가 반영된 크로모좀을 리턴
		for(int chromosome_index=0;chromosome_index<input_array.size() ; chromosome_index++) {
			input_array.set(chromosome_index, eqArrange(input_array.get(chromosome_index)));
			calcFitness_renew(input_array.get(chromosome_index));
			//System.out.println(input_array.get(chromosome_index).fitness_value);
		}
		
		//뒤집는 부분 있어서 가지고 옴
//		convertEQCoord(population);
//		convertEQCoord();
//		convertFixedEqCoord();
//		convertZoneCoord();
//		
//		
//		convertEQCoord(population);
////		convertEQCoord();
//		convertZoneCoord();
//		convertFixedEqCoord();
		
		
	}
//	초기 population을 생성하는 코드
	public void gen_population()
	{
		for(int i = 0; i < population_number ; i++) {
			
			Renewal_MicroOrganism individual_chromosome = new Renewal_MicroOrganism();
			
			ArrayList<CModule> temp_list = (ArrayList<CModule>) resultSet.direct_PRC_order.clone();
			int start_ind = 0;
			for(int num_prc : resultSet.direct_PRC_number) {
				int to_ind = start_ind + num_prc;
				Collections.shuffle(temp_list.subList(start_ind, to_ind)); 
				start_ind = to_ind;
			}		
			individual_chromosome.set_each_chromosome(temp_list);
			
			n_lstPopulation.add(individual_chromosome);
		}
	}
	
	
//	Crossover할 크로모좀 하나를 selection하는 코드
	public Renewal_MicroOrganism selection()
	{
		make_lst_roulette();
		
		Random random = new Random();
		
		double select_probability = random.nextDouble() * lst_roulette.get(lst_roulette.size()-1);
		
		boolean stop_value = false;
		
		int select_index = 0;
		int selected_index = -1;
		double prev_number = 0;
		for(double each_roul : lst_roulette) {
			if(prev_number < select_probability && select_probability <= each_roul) {
				selected_index = select_index;
				stop_value = true;
			}
			if(stop_value) {
				break;
			}
			select_index++;
			prev_number = each_roul;
		}

		return n_lstPopulation.get(selected_index);
	}
	

	
//	룰렛 휠 방식을 위한 누적 확률list 만들기(정확히 확률은 아님,마지막 원소가 1로 끝나진 않음)
	public void make_lst_roulette() {
		lst_roulette = new ArrayList<Double>();
		
//		각 룰렛값들의 누적합을 담는 list 생성(lst_roulette) 
		double prev_value = 0.0;
		for(Renewal_MicroOrganism popul : n_lstPopulation) {
			double next_value = prev_value + popul.roulette_probability;
			lst_roulette.add(next_value);
			prev_value += popul.roulette_probability;
		}
	}
	
	
//	2개의 microorganism을 인풋받아 [자손 microorganism,자손 microorganism]으로 return해주는 함수
	public Renewal_MicroOrganism[] crossover(Renewal_MicroOrganism parent1,Renewal_MicroOrganism parent2 )
	{
		Random random = new Random();
		
//		0(포함) ~ n-1 까지의 정수 생성. 지금은 11로 하드코딩 했는데 resultSet.getM_lstRoomOrder() 잘 작동하면 이걸로 바꾸던지 해야함
		int changer_index =  random.nextInt(facil_number-2)+1;
		
		
		int prc_index =0;
		for(int i=0;i<changer_index;i++){
			prc_index += resultSet.direct_PRC_number.get(i);
		}
		//System.out.println(prc_index);
		
		Renewal_MicroOrganism temp_descendent1 = new Renewal_MicroOrganism();
		Renewal_MicroOrganism temp_descendent2 = new Renewal_MicroOrganism();
		
		temp_descendent1.each_chromosome.addAll(parent1.each_chromosome.subList(0, prc_index));
		temp_descendent1.each_chromosome.addAll(parent2.each_chromosome.subList(prc_index,parent2.each_chromosome.size()));
		
		temp_descendent2.each_chromosome.addAll(parent2.each_chromosome.subList(0, prc_index));
		temp_descendent2.each_chromosome.addAll(parent1.each_chromosome.subList(prc_index,parent2.each_chromosome.size()));
		
		Renewal_MicroOrganism[] re_array = {temp_descendent1, temp_descendent2};
		return re_array;
	}
	
	
//	Mutation하는 코드
	public ArrayList<Renewal_MicroOrganism> mutation(ArrayList<Renewal_MicroOrganism> population)
	{
		    Random rand = new Random();
		    
//		    뽑힐 확률
		    double selected_probaility = 0.01;

//    		arraylist를 순회하면서 확률값에 의해 muatation대상이 될수도 안될수도
		    for(int i=0;i<population.size();i++) {
		    	if(rand.nextDouble() < selected_probaility) {
		    		
					//System.out.println("");
					for (int k = 0; k < population.get(i).each_chromosome.size(); k++)
					{
					//System.out.print(population.get(i).each_chromosome.get(k).getM_strName()+" ");
					}
					//System.out.println("");
		    		
		    		int facil_slect =  rand.nextInt(facil_number-1);
		    		
		    		int prc_index = 0;
		    		int prc_num = resultSet.direct_PRC_number.get(0);
		    		
		    		for(int j=0;j < facil_slect;j++){
//		    			prc_index += resultSet.direct_PRC_number.get(j);
		    			prc_index += prc_num;
		    			prc_num =  resultSet.direct_PRC_number.get(j+1);
		    		}
		    		
		    		int first_selected_prc = rand.nextInt(prc_num-1)+prc_index;
		    		CModule move_1 = population.get(i).each_chromosome.get(first_selected_prc);
		    		int second_selected_prc;
		    		CModule move_2;
		    		
//		    		혹시 같은 index가 뽑힐 수 있으니까
		    		do{
		    			second_selected_prc = rand.nextInt(prc_num-1)+prc_index;
		    			move_2 = population.get(i).each_chromosome.get(second_selected_prc);
		    		} while(first_selected_prc == second_selected_prc); 
		    		
		    		//System.out.println("뽑힌인덱스");
		    		//System.out.println(first_selected_prc);
		    		//System.out.println(second_selected_prc);
		    		
		    		population.get(i).each_chromosome.set(first_selected_prc,move_2);
		    		population.get(i).each_chromosome.set(second_selected_prc,move_1);
//					System.out.println("바뀌기전");
//					for (i = 0; i < selected_one.each_chromosome.size(); i++)
//					{
//					System.out.print(selected_one.each_chromosome.get(i).getM_strName()+" ");
//					}				
					//System.out.println("");
					for (int k = 0; k < population.get(i).each_chromosome.size(); k++)
					{
					//System.out.print(population.get(i).each_chromosome.get(k).getM_strName()+" ");
					}
					//System.out.println("");
		    		
		    		
		    	}
			}
		    
		    
		    return population;
	}
	
//	population중 가장 좋은 크로모좀을 return해주는 함수
	public Renewal_MicroOrganism find_best_chromosome(ArrayList<Renewal_MicroOrganism> population){
		
		double best_solution = 10000000000.00;
		int best_index = -1;
//		fitness가 큰게 좋은지 작은지 좋은지에 따라 달라지기는 하지만 우선은 작은게 좋다고 함
		for(int i=0;i<population.size();i++) {
			if(population.get(i).fitness_value < best_solution) {
				best_solution = population.get(i).fitness_value;
				best_index = i;
			}
		}
		return population.get(best_index);
	}
	
	
	
	
	public Renewal_MicroOrganism eqArrange(Renewal_MicroOrganism chromosome)
	{
		//Renewal_MicroOrganism안에 each_chromosome(PRC순서의 gene)을 EQ별로 쪼개줌
		ArrayList<EQ> EQ_list = new ArrayList<EQ>();
			
		
		// phase1의 결과에 맞게금 List를 만들어주는 부분
		
		int[] using_index = new int[11];
		ArrayList<int[]> facil_start_index = new ArrayList<int[]>();
		int[] start_array = {0,0};
		facil_start_index.add(start_array);
		
		//*--joon_fix 지금은 하드코딩있음 
		String prev_index = "BB";
		int cumulated_index = 0;
		
		
		for(int find_start_index = 0;find_start_index < chromosome.each_chromosome.size();find_start_index++) {	
			if(chromosome.each_chromosome.get(find_start_index).getM_strRoom().equals(prev_index)==false){
				prev_index = chromosome.each_chromosome.get(find_start_index).getM_strRoom();
				int[] input_array = {cumulated_index,0};
				facil_start_index.add(input_array);
			}
			cumulated_index++;
		}
		
		for(int[] phase1_result : resultSet.direct_facil_order) {
//		phase1 결과읽어올때는 A : 1, B : 2 ...11까지 있음 index 햇갈림 주의
			
			int number_of_installing = phase1_result[1];
			boolean continuing = true;
			
			if(phase1_result[1]==0) {
				continuing = false;
			}
			int calling_index = facil_start_index.get(phase1_result[0]-1)[0];
			int calling_index_remaining = facil_start_index.get(phase1_result[0]-1)[1];	
			while(continuing) {
				// calling_index_remaining index가 0보다 큼 => 즉 이전 배치에서 남은 설비가 있음
				if(calling_index_remaining > 0) {
					
//					한PRC내 EQ들이 통째로 들어갈때
					if(number_of_installing - chromosome.each_chromosome.get(calling_index).getM_lstEQ().size() + calling_index_remaining >= 0) {
						EQ_list.addAll(chromosome.each_chromosome.get(calling_index).getM_lstEQ().subList(calling_index_remaining,chromosome.each_chromosome.get(calling_index).getM_lstEQ().size()));
						number_of_installing = number_of_installing - chromosome.each_chromosome.get(calling_index).getM_lstEQ().size() + calling_index_remaining;
						calling_index_remaining = 0;
						calling_index++;
					}else {
//						한 공정내에 전부다가 아니고 일부가 들어갈때 - 다 넣고 잘리는 index도 보관 =>facil_start_index
						EQ_list.addAll(chromosome.each_chromosome.get(calling_index).getM_lstEQ().subList(calling_index_remaining,calling_index_remaining + number_of_installing));
						int[] revise_array = {calling_index,calling_index_remaining + number_of_installing};
						facil_start_index.set(phase1_result[0]-1,revise_array);  
						continuing = false;
					}
					
				// calling_index_remaining index가 0 => 즉 이전 배치에서 남은 설비가 있는게 아니고 새로 시작하는거
				}else { 
					if(number_of_installing - chromosome.each_chromosome.get(calling_index).getM_lstEQ().size() >= 0) {
//						한PRC내 EQ들이 통째로 들어갈때
						EQ_list.addAll(chromosome.each_chromosome.get(calling_index).getM_lstEQ());
						number_of_installing = number_of_installing - chromosome.each_chromosome.get(calling_index).getM_lstEQ().size();
						calling_index++;
					}else {
//						한 공정내에 전부다가 아니고 일부가 들어갈때 - 다 넣고 잘리는 index도 보관 =>facil_start_index
						EQ_list.addAll(chromosome.each_chromosome.get(calling_index).getM_lstEQ().subList(0,number_of_installing));
						int[] revise_array = {calling_index,number_of_installing};
						facil_start_index.set(phase1_result[0]-1,revise_array);  
						continuing = false;
					}
				}
				if(calling_index>=chromosome.each_chromosome.size()) {
					continuing = false;
				}
			}
		}
		
		
		// 설비배치를 하기 위한 배치순서를 배열로 변겅.
		String[] EQBatchOrder = new String[EQ_list.size()];

		for (int i = 0; i < EQ_list.size(); i++)
		{
//			EQBatchOrder[i] = population.getChromosomeByName().get(i).getName();
			EQBatchOrder[i] = EQ_list.get(i).getDeviceName();
		}
		/*2019.7.15 Review
		 * 설비의 위치를 결정할 때 쓰이는 부분:CEQArrangeInRoom
		 * */
		CEQArrangeInRoom arranger = new CEQArrangeInRoom();
		

		// ksn 테스트 위해서 코드 변경 (2010.0729). 완성후 에는 삭제 필요함.
		ArrayList<CBay> bayList = null;
		if(CDataSet.RUNTEST) {
//
//			 if (room.getM_strName().equals("CMP"))
//			 {
//			 bayList = mi.go(room, TestData.CMPEQBatchOrder);
//			 } else if (room.getM_strName().equals("METAL"))
//			 {
//			 bayList = mi.go(room, TestData.METALEQbatchOrder);
//			 } else if (room.getM_strName().equals("TEST"))
//			 {
//			 bayList = mi.go(room, TestData.TESTEQBatchOrder);
//			 } else if (room.getM_strName().equals("CLEAN"))
//			 {
//			 bayList = mi.go(room, TestData.CLEANEQBatchOrder);
//			 } else if (room.getM_strName().equals("CVD"))
//			 {
//			 bayList = mi.go(room, TestData.CVDEQBatchOrder);
//			 } else if (room.getM_strName().equals("PHOTO"))
//			 {
//			 bayList = mi.go(room, TestData.PHOTOEQBatchOrder);
//			 } else if (room.getM_strName().equals("DIFF"))
//			 {
//			 bayList = mi.go(room, TestData.DIFFEQBatchOrder);
//			 } else if (room.getM_strName().equals("ETCH"))
//			 {
//			 bayList = mi.go(room, TestData.ETCHEQBatchOrder);
//			 }
		}
		else {
//			CEQArrangeInRoom에서 room은 그냥 bay가 어느 room에 속하는지만 정해주는것 같은 개념인듯 싶어서 우선 임의의 room으로 배치 *--joon_fix
			CRoom any_room = new CRoom();
			bayList = arranger.go(any_room, EQBatchOrder);
		}
		
		// Population의 크로모좀에서 EQ데이터를 좌표가 기록되어 있는 EQ Data로 변경함.
		chromosome.getChromosomeByName().clear();
		for (int i = 0; i < bayList.size(); i++)
		{
			chromosome.getChromosomeByName().addAll(bayList.get(i).getListEq());
		}
		chromosome.setM_lstBay(bayList);
		return chromosome;
	}	
	
	public Renewal_MicroOrganism calcFitness_renew(Renewal_MicroOrganism chromosome) {
		//ksn Fitness 수정 필요. 수정 방향 -> 거리와 면적의 조합?

		double fitnessByArea = 0;
//		double fitnessByDistance = 0;
		//가로 방향 배치인 경우 돌려서 계산하고 다시 돌려 놓는다.
		
		convertEQCoord(chromosome);
		convertEQCoord();
		convertFixedEqCoord();
		convertZoneCoord();
		
		// 공정 안에 설비를 배치하고 적합도(이동동선, CrossOver)를 계산한다.
		//fitnessByArea = calFitnessByArea(population, isLastRoom);
		//population.setFitnessByArea(fitnessByArea);

		chromosome = calFitnessByDistance_renew(chromosome);
//		//population.setFitnessByDistance(fitnessByDistance);
		
		
		convertEQCoord(chromosome);
		convertEQCoord();
		convertZoneCoord();
		convertFixedEqCoord();
		return chromosome;
	}
	
	/**
	 * 
	 * @author : kyveri.kim
	 * @version : 2010. 07. 21 오후 5:00:15
	 * @date : 2010. 07. 21
	 * @param population
	 * @변경이력 :
	 * @Method 설명 : Population의 EQ 적합도(이동동선, CrossOver)를 계산함.
	 */
	public MicroOrganism calcFitness(MicroOrganism population, CRoom curRoom, boolean isLastRoom)
	{
		//ksn Fitness 수정 필요. 수정 방향 -> 거리와 면적의 조합?

		double fitnessByArea = 0;
//		double fitnessByDistance = 0;
		//가로 방향 배치인 경우 돌려서 계산하고 다시 돌려 놓는다.
		
		//convertEQCoord(population);
		convertEQCoord();
		convertFixedEqCoord();
		convertZoneCoord();
		
		// 공정 안에 설비를 배치하고 적합도(이동동선, CrossOver)를 계산한다.
		fitnessByArea = calFitnessByArea(population, isLastRoom);
		population.setFitnessByArea(fitnessByArea);

		population = calFitnessByDistance(population, curRoom);
//		population.setFitnessByDistance(fitnessByDistance);
		
		
		//convertEQCoord(population);
		convertEQCoord();
		convertZoneCoord();
		convertFixedEqCoord();
		return population;
	}
	
	
	
//======================= ===============================================================================================================
//	
//======================= ===============================================================================================================
//	
//	여기 부터는 기존 코드
//	
//======================= ===============================================================================================================
//	
//======================= ===============================================================================================================
	
	
	
	public ArrayList<MicroOrganism> run(CRoom room, LogFile logFile)
	{
		System.out.println("** 공정 [" + room.getM_strName() + "]에 대한 설비 배치 시작");		
		int iGen = 0;
		int var1 = 0, var2 = 0;
		MicroOrganism population;
		// parameter 설정
		initGAParameter(room);
		// 초기해 설정
		// 거리 기준으로 모든 것을 계산. 면적 반영을 위한 코드 추가해야 함.
		// 현재: Fitness Function = FF_거리
		// 이후: FF = FF_거리 * p + FF_면적 * (1-p) 이렇게 하면 scale이 다르다는 문제가 있음.
		// 대안: FF = FF_거리/최단거리 * p + FF_면적/최소면적 * (1-p), 최소 거리와 최소 면적을 무엇으로 할 지에 대한 문제.
		// 최소거리는 해당 Room과 다른 Room 중심간의 직각 거리, 최소 면적은 EQ의 넓이(여유공간포함)의 합.
		initialize();
		pastFitnessFunctionValue = 1000000000; // fitness func. 초기값
		currentFitnessFunctionValue = 999999999;
		isBetterSolution = true; // 더 좋은 값이 나오면 결과값을 찍어보자.
		int searchCnt = 0;
		ArrayList<Integer> searchHistory = new ArrayList<Integer>();
//		for (int i = 0; i < lstPopulations.size(); i++) {
//			System.out.println("getFitnessByDistance():" + lstPopulations.get(i).getFitnessByDistance());	
//		}
		// maxGenerations 만큼 GA 수행 (terminate 조건)
		while (iGen < CDataSet.MICRO_MAX_GENERATION)
		{
			iGen++;

			//가장 적은 거리 순으로 배치
			sortPopulationsByDistance();
			
			//가장 적은 거리를 반송하는 설비배치 순서를 출력함
			printOrganism(0);
			//설비 배치 필요면적이 작은 모수들을 걸러냄
			
			elitism();
			for (int i = 0; i < CDataSet.MICRO_POPULATIONS_SIZE - buffer.size(); i++)
			{
				// select operator를 통해 교차할 두 부모를 선택
				var1 = getRandomPopulation();
				var2 = getRandomPopulation();
				// 교차 수행
				population = crossover(var1, var2);
				
				// *--joon_fix mutation도 현재 부분에서는 의미가 없어서 주석처리 해두었습니다
//				population = mutate(population);

				// population의 EQ를 순서에 맞게 Room에 배치함.
//				population = eqArrange(population);
				
				// Population의 EQ 적합도(이동동선, CrossOver)를 계산함
				calcFitness(population, room, room.isLastRoom());
				
				// // 교차와 변이로 생성된 자식 개체가 feasible 하지 않을 경우 수선

				// boolean isFeasibleChromosome = population.getFeasible(room);
				// if (!isFeasibleChromosome) repair(chromosom);
				buffer.add(population);
			}
			if(isBetterSolution){
				System.out.println("Generation number : " + iGen);
				searchHistory.add(searchCnt);
				searchCnt = 0;
			} else {
				searchCnt++;
			}
			swap();
			
			if(searchCnt>CDataSet.MICRO_SEARCH_LIMIT){
				System.out.println("No better solution in [" + (searchCnt - 1) + "] generations");				
				break; // 얼마 이상 찾아도 값이 개선되지 않으면 종료
			}
			//---*
		} // End while
		sortPopulationsByDistance();
		StringBuffer strSearchHistory = new StringBuffer();
		strSearchHistory.append("해 개선 횟수 [").append(searchHistory.size()).append("] 회\r\n");
		strSearchHistory.append("개선 시점:");
		for (int i = 0; i < searchHistory.size(); i++) {
			strSearchHistory.append("[").append(searchHistory.get(i)).append("]");
		}
		if(logFile!=null) logFile.append(strSearchHistory.toString());
		if(logFile!=null) logFile.append("공정 " + room.getM_strName() + "에 대한 설비배치 순서 (설비명/모듈명)");
		printOrganism(0, logFile);
		
		if(logFile!=null) logFile.append("공정 " + room.getM_strName() + "에 대한 설비배치 종료");
		// 결과를 출력하고 저장함.
		finish();
		
		return lstPopulations;
	}
	
//	/**
//	 * 
//	 * @author : jwon.cho
//	 * @version : 2011. 05. 18 오전 9:54:34
//	 * @param logFile 
//	 * @date : 2011. 05. 18
//	 * @변경이력 :
//	 * @Method 설명 : GA를 Run 하는 Method
//	 */
//	public ArrayList<MicroOrganism> run(CRoom room, LogFile logFile, ArrayList<MicroOrganism> initPopulation, int currentRoomIndex)
//	{
//System.out.println("** 공정 [" + room.getM_strName() + "]에 대한 설비 배치 시작");		
//		int iGen = 0;
//		int var1 = 0, var2 = 0;
//		MicroOrganism population;
//		// parameter 설정
//		initGAParameter(room);
//		// 초기해 설정
//		// 거리 기준으로 모든 것을 계산. 면적 반영을 위한 코드 추가해야 함.
//		// 현재: Fitness Function = FF_거리
//		// 이후: FF = FF_거리 * p + FF_면적 * (1-p) 이렇게 하면 scale이 다르다는 문제가 있음.
//		// 대안: FF = FF_거리/최단거리 * p + FF_면적/최소면적 * (1-p), 최소 거리와 최소 면적을 무엇으로 할 지에 대한 문제.
//		// 최소거리는 해당 Room과 다른 Room 중심간의 직각 거리, 최소 면적은 EQ의 넓이(여유공간포함)의 합.
//		initialize(initPopulation);
//		pastFitnessFunctionValue = 1000000000; // fitness func. 초기값
//		currentFitnessFunctionValue = 999999999;
//		isBetterSolution = true; // 더 좋은 값이 나오면 결과값을 찍어보자.
//		int searchCnt = 0;
//		ArrayList<Integer> searchHistory = new ArrayList<Integer>();
////		for (int i = 0; i < lstPopulations.size(); i++) {
////			System.out.println("getFitnessByDistance():" + lstPopulations.get(i).getFitnessByDistance());	
////		}
//		// maxGenerations 만큼 GA 수행 (terminate 조건)
//		while (iGen < CDataSet.MICRO_MAX_GENERATION)
//		{
//			iGen++;
//
//			//가장 적은 거리 순으로 배치
//			sortPopulationsByDistance();
//			
//			//가장 적은 거리를 반송하는 설비배치 순서를 출력함
//			printOrganism(0);
//			//설비 배치 필요면적이 작은 모수들을 걸러냄
//			
//			elitism();
//			for (int i = 0; i < CDataSet.MICRO_POPULATIONS_SIZE - buffer.size(); i++)
//			{
//				// select operator를 통해 교차할 두 부모를 선택
//				var1 = getRandomPopulation();
//				var2 = getRandomPopulation();
//				// 교차 수행
//				population = crossover(var1, var2);
//				population = mutate(population);
//
//				// population의 EQ를 순서에 맞게 Room에 배치함.
//				population = eqArrange(population);
//				
//				// Population의 EQ 적합도(이동동선, CrossOver)를 계산함
//				calcFitness(population, room, room.isLastRoom());
//				
//				// // 교차와 변이로 생성된 자식 개체가 feasible 하지 않을 경우 수선
//
//				// boolean isFeasibleChromosome = population.getFeasible(room);
//				// if (!isFeasibleChromosome) repair(chromosom);
//				buffer.add(population);
//			}
//			if(isBetterSolution){
//				System.out.println("Generation number : " + iGen);
//				searchHistory.add(searchCnt);
//				searchCnt = 0;
//			} else {
//				searchCnt++;
//			}
//			swap();
//			if(searchCnt>CDataSet.MICRO_SEARCH_LIMIT){
//System.out.println("No better solution in [" + (searchCnt - 1) + "] generations");				
//				break; // 얼마 이상 찾아도 값이 개선되지 않으면 종료
//			}
//		} // End while
//		sortPopulationsByDistance();
//		StringBuffer strSearchHistory = new StringBuffer();
//		strSearchHistory.append("해 개선 횟수 [").append(searchHistory.size()).append("] 회\r\n");
//		strSearchHistory.append("개선 시점:");
//		for (int i = 0; i < searchHistory.size(); i++) {
//			strSearchHistory.append("[").append(searchHistory.get(i)).append("]");
//		}
//		if(logFile!=null) logFile.append(strSearchHistory.toString());
//		if(logFile!=null) logFile.append("공정 " + room.getM_strName() + "에 대한 설비배치 순서 (설비명/모듈명)");
//		printOrganism(0, logFile);
//		
//		if(logFile!=null) logFile.append("공정 " + room.getM_strName() + "에 대한 설비배치 종료");
//		// 결과를 출력하고 저장함.
//		finish();
//		
//		return lstPopulations;
//	}
	
	/**
	 * 
	 * @author : kyveri.kim
	 * @version : 2010. 07. 13 오전 9:54:34
	 * @param logFile 
	 * @date : 2010. 07. 13
	 * @변경이력 : jwon.cho 추가 - 면적 베이스
	 * @Method 설명 : GA를 Run 하는 Method
	 */
	public ArrayList<MicroOrganism> runArea(CRoom room, LogFile logFile)
	{
		int iGen = 0;
		int var1 = 0, var2 = 0;
		MicroOrganism population;
		// parameter 설정
		initGAParameter(room);
		// 초기해 설정
		// 거리 기준으로 모든 것을 계산. 면적 반영을 위한 코드 추가해야 함.
		// 현재: Fitness Function = FF_거리
		// 이후: FF = FF_거리 * p + FF_면적 * (1-p) 이렇게 하면 scale이 다르다는 문제가 있음.
		// 대안: FF = FF_거리/최단거리 * p + FF_면적/최소면적 * (1-p), 최소 거리와 최소 면적을 무엇으로 할 지에 대한 문제.
		// 최소거리는 해당 Room과 다른 Room 중심간의 직각 거리, 최소 면적은 EQ의 넓이(여유공간포함)의 합.
		initialize();
		pastFitnessFunctionValue = 0; // fitness func. 초기값
		currentFitnessFunctionValue = 1;
		isBetterSolution = true; // 더 좋은 값이 나오면 결과값을 찍어보자.
		int searchCnt = 0;
//		for (int i = 0; i < lstPopulations.size(); i++) {
//			System.out.println("getFitnessByDistance():" + lstPopulations.get(i).getFitnessByDistance());	
//		}
		// maxGenerations 만큼 GA 수행 (terminate 조건)
		while (iGen < CDataSet.MICRO_MAX_GENERATION)
		{
			iGen++;

			//가장 적은 거리 순으로 배치
			sortPopulationsByArea();
			
			//가장 적은 거리를 반송하는 설비배치 순서를 출력함
			printOrganismArea(0);
			//설비 배치 필요면적이 작은 모수들을 걸러냄
			elitism();
			for (int i = 0; i < CDataSet.MICRO_POPULATIONS_SIZE - buffer.size(); i++)
			{
				// select operator를 통해 교차할 두 부모를 선택
				var1 = getRandomPopulationArea();
				var2 = getRandomPopulationArea();
				// 교차 수행
				population = crossover(var1, var2);
				population = mutate(population);

				// population의 EQ를 순서에 맞게 Room에 배치함.
//				population = eqArrange(population);
				
				// Population의 EQ 적합도(이동동선, CrossOver)를 계산함
				calcFitness(population, room, room.isLastRoom());
				
				// // 교차와 변이로 생성된 자식 개체가 feasible 하지 않을 경우 수선

				// boolean isFeasibleChromosome = population.getFeasible(room);
				// if (!isFeasibleChromosome) repair(chromosom);
				buffer.add(population);
			}
			if(isBetterSolution){
				System.out.println("Generation number : " + iGen);
				searchCnt = 0;
			} else {
				searchCnt++;
			}
			swap();
			if(searchCnt>CDataSet.MICRO_SEARCH_LIMIT){
System.out.println("searchCnt:" + searchCnt);				
				break; // 얼마 이상 찾아도 값이 개선되지 않으면 종료
			}
		} // End while
		sortPopulationsByArea();
		if(logFile!=null) logFile.append("공정 " + room.getM_strName() + "에 대한 설비배치 순서 (설비명/모듈명)");
		printOrganismArea(0, logFile);
		
		if(logFile!=null) logFile.append("공정 " + room.getM_strName() + "에 대한 설비배치 종료");
		// 결과를 출력하고 저장함.
		finishArea();
		
		return lstPopulations;
	}
	/**
	 * 
	 * @author : kyveri.kim
	 * @version : 2010. 07. 21 오후 4:56:41
	 * @date : 2010. 07. 21
	 * @param population
	 * @변경이력 :
	 * @Method 설명 : Eq를 순서에 맞게 배치함.
	 */
	/*
	 * EQBatchOrder는 EQ 순서를 String으로 갖고 있다.
	 * 중복..
	 * */
	

	/**
	 * 
	 * @author : kyveri.kim
	 * @version : 2010. 07. 19 오전 8:50:40
	 * @date : 2010. 07. 19
	 * @변경이력 :
	 * @Method 설명 : 인구(Population, 생물체의 모임) 버퍼의 값을 원래 인구(Population, 생물체의 모임) 변수에 복사합니다. 즉 새로운 세대(New Generation)를 만듭니다.
	 */
	private void swap()
	{
		lstPopulations.clear();
		lstPopulations.addAll(buffer);
		buffer.clear();
	}

	/**
	 * 
	 * @author : kyveri.kim
	 * @version : 2010. 07. 19 오전 9:00:33
	 * @date : 2010. 07. 19
	 * @param row
	 * @변경이력 :
	 * @Method 설명 : row 번째 Chromosome의 배열과 순서를 입력받음. 해가 개선된 경우 출력
	 */
	private void printOrganism(int row)
	{
		MicroOrganism c = (MicroOrganism) lstPopulations.get(row);
		ArrayList<Integer> chromosome = c.getChromosomeByInt();
		
		currentFitnessFunctionValue = c.getIncrementalFitnessByDistance();
		if(currentFitnessFunctionValue < pastFitnessFunctionValue) {
			pastFitnessFunctionValue = currentFitnessFunctionValue;
			isBetterSolution = true;
		} else {
			isBetterSolution = false;
		}
		
		if(isBetterSolution) {
			// System.out.print("[" + c.getFitnessByArea() + "]");
			StringBuffer prtStr = new StringBuffer();
			prtStr.append("[fitness:" + c.getFitnessByDistance() + "/ "+ c.getFitnessByDistance()/CDataSet.getInstance().fab_total_volume*1000 +"]");
			prtStr.append("[incrementalF:" + c.getIncrementalFitnessByDistance() + "/ "+ c.getIncrementalFitnessByDistance()/CDataSet.getInstance().fab_total_volume*1000 +"]");
			for (int i = 0; i < chromosome.size(); i++)
				prtStr.append(chromosome.get(i) + " ");
			prtStr.append("\n");
			System.out.println(prtStr.toString());
		}
	}
	/**
	 * 
	 * @author : kyveri.kim
	 * @version : 2010. 07. 19 오전 9:00:33
	 * @date : 2010. 07. 19
	 * @param row
	 * @변경이력 :
	 * @Method 설명 : row 번째 Chromosome의 배열과 순서를 입력받음. 해를 파일로 출력
	 */
	private void printOrganism(int row, LogFile logFile)
	{
		MicroOrganism c = (MicroOrganism) lstPopulations.get(row);
		ArrayList<Integer> chromosome = c.getChromosomeByInt();
		ArrayList<EQ> chromosomeEQ = c.getChromosomeByName();
		
		StringBuffer prtStr = new StringBuffer();
		prtStr.append("[fitness:" + c.getFitnessByDistance() + "/ "+ c.getFitnessByDistance()/CDataSet.getInstance().fab_total_volume*1000 +"]");
		prtStr.append("[incrementalF:" + c.getIncrementalFitnessByDistance() + "/ "+ c.getIncrementalFitnessByDistance()/CDataSet.getInstance().fab_total_volume*1000 +"]");
//		prtStr.append("[" + c.getFitnessByDistance() + "]");
		for (int i = 0; i < chromosome.size(); i++)
			prtStr.append(chromosome.get(i) + " ");
		prtStr.append("\n");
		System.out.println(prtStr.toString());
		
		StringBuffer logStr = new StringBuffer();
		for (int i = 0; i < chromosomeEQ.size(); i++)
			logStr.append(chromosomeEQ.get(i).getModule() + "/" + chromosomeEQ.get(i).getDeviceName() + " ");
		
		if(logFile!=null) logFile.append(logStr.toString());

	}
	/**
	 * 
	 * @author : kyveri.kim
	 * @version : 2010. 07. 19 오전 9:00:33
	 * @date : 2010. 07. 19
	 * @param row
	 * @변경이력 :
	 * @Method 설명 : row 번째 Chromosome의 배열과 순서를 입력받음. 해가 개선된 경우 출력
	 */
	private void printOrganismArea(int row)
	{
		MicroOrganism c = (MicroOrganism) lstPopulations.get(row);
		ArrayList<Integer> chromosome = c.getChromosomeByInt();
		ArrayList<EQ> chromosomeEQ = c.getChromosomeByName();
		
		currentFitnessFunctionValue = c.getFitnessByArea();
		if(currentFitnessFunctionValue < pastFitnessFunctionValue) {
			pastFitnessFunctionValue = currentFitnessFunctionValue;
			isBetterSolution = true;
		} else {
			isBetterSolution = false;
		}
		
		if(isBetterSolution) {
			// System.out.print("[" + c.getFitnessByArea() + "]");
			StringBuffer prtStr = new StringBuffer();
			prtStr.append("[" + c.getFitnessByArea() + "]");
			for (int i = 0; i < chromosome.size(); i++)
				prtStr.append(chromosome.get(i) + " ");
			prtStr.append("\n");
			System.out.println(prtStr.toString());
		}
	}
	/**
	 * 
	 * @author : kyveri.kim
	 * @version : 2010. 07. 19 오전 9:00:33
	 * @date : 2010. 07. 19
	 * @param row
	 * @변경이력 :
	 * @Method 설명 : row 번째 Chromosome의 배열과 순서를 입력받음. 해를 파일로 출력
	 */
	private void printOrganismArea(int row, LogFile logFile)
	{
		MicroOrganism c = (MicroOrganism) lstPopulations.get(row);
		ArrayList<Integer> chromosome = c.getChromosomeByInt();
		ArrayList<EQ> chromosomeEQ = c.getChromosomeByName();
		
		StringBuffer prtStr = new StringBuffer();
		prtStr.append("[" + c.getFitnessByArea() + "]");
		for (int i = 0; i < chromosome.size(); i++)
			prtStr.append(chromosome.get(i) + " ");
		prtStr.append("\n");
		System.out.println(prtStr.toString());
		
		StringBuffer logStr = new StringBuffer();
		for (int i = 0; i < chromosomeEQ.size(); i++)
			logStr.append(chromosomeEQ.get(i).getModule() + "/" + chromosomeEQ.get(i).getDeviceName() + " ");
		
		if(logFile!=null) logFile.append(logStr.toString());

	}
	/**
	 * @author : kyveri.kim
	 * @version : 2010. 07. 19 오전 9:31:07
	 * @date : 2010. 07. 19
	 * @변경이력 :
	 * @Method 설명 : fitness Rank가 우수한 값부터 내림차순으로 정렬함.
	 */
	private void sortPopulationsByArea()
	{
		Collections.sort(lstPopulations, new AreaComparator());

		Collections.reverse(lstPopulations);

	}
	/**
	 * @author : kyveri.kim
	 * @version : 2010. 07. 19 오전 9:31:07
	 * @date : 2010. 07. 19
	 * @변경이력 :
	 * @Method 설명 : fitness Rank가 우수한 값부터 내림차순으로 정렬함.
	 */
	private void sortPopulationsByDistance()
	{
		Collections.sort(lstPopulations, new DistanceComparator());
	}

	/**
	 * 
	 * @author : kyveri.kim
	 * @version : 2010. 07. 12 오후 5:04:26
	 * @date : 2010. 07. 12
	 * @param maxgenera
	 *            최대 자손의 수
	 * @param popul
	 *            모집단
	 * @param room
	 *            Room의 데이터
	 * @param fab
	 *            패널의 정보.
	 * @변경이력 :
	 * @Method 설명 : parameter 설정
	 */
	private void initGAParameter(CRoom ro)
	{
		// 모집단의 수만큼 chromosom을 생성하기 위한 배열을 Chromosom List 생성.
		lstPopulations = new ArrayList<MicroOrganism>();
		room = ro;
		moduleList = room.getM_lstModule();
//		lstPopulations = new ArrayList<MicroOrganism>();
		buffer = new ArrayList<MicroOrganism>();

		propertiesByInt = new HashMap<Integer, CModule>();
		propertiesByName = new HashMap<String, Integer>();

		// 정수를 모듈과 하나씩 mapping
		int putSize = moduleList.size();
		for (int i = 0; i < putSize; i++)
		{
			propertiesByInt.put(i, moduleList.get(i));
			propertiesByName.put(moduleList.get(i).getM_strName(), i);

		}

		int moduleNum = room.getM_nModuleCount(); // 모듈의 수
		moduleEqNum = new int[moduleNum]; // 모듈별 설비들의 수

		for (int j = 0; j < moduleNum; j++)
		{
			moduleEqNum[j] = room.getM_lstModule().get(j).getM_nEQCount();
		}
	}

	/**
	 * 
	 * @author : kyveri.kim
	 * @version : 2010. 07. 13 오전 9:45:22
	 * @date : 2010. 07. 13
	 * @변경이력 :
	 * @Method 설명 : 초기해 설정, GA를 사용하기 위해 주어진 layout에서의 공장크기 및 설비들로 random하게 population을 만들었으나, 선정된 layout 내에서 repair
	 *         operation 이나 random이 아닌 다른 방법으로 initial solutions 을 설정하는 것이 좋을 것으로 보임
	 * 
	 */
	private void initialize()
	{
		int EqNum = room.getM_lstEQ().size(); // 전체 설비들의 수
		int moduleNum = room.getM_lstModule().size(); // 모듈의 수
		int[] tempModuleEqNum = new int[moduleNum]; // 모듈별 설비수
		boolean isLastRoom = room.isLastRoom();
		ArrayList<Integer> chromosomeByInt;
		ArrayList<EQ> chromosomeByName;

		MicroOrganism population;

		/*2019.07.11 임대은 review
		 * chromosomeByInt는 모듈번호를 담는다. 예를 들어 3,1,0,2,2
		 * 1st 설비는 3번 모듈에서 뽑으라는 얘기
		 * 2nd 설비는 1번 모듈에서 뽑으라는 얘기
		 * 모듈이 PRC 그룹이라면 이렇게하면 안될듯! 이때는 모듈을 왜 나눠 놓은거지..
		 * */
		// 모듈별 설비수를 넘지 않게 초기해를 구하는 과정
		for (int i = 0; i < CDataSet.MICRO_POPULATIONS_SIZE; i++)
		{
			chromosomeByInt = new ArrayList<Integer>(EqNum); // solution
			chromosomeByName = new ArrayList<EQ>(EqNum); // solution

			for (int j = 0; j < moduleNum; j++)
				tempModuleEqNum[j] = moduleEqNum[j];

			for (int j = 0; j < EqNum; j++)
			{ // 모듈별 설비들의 수를 넘지 않는 한에서 랜덤하게 배치
				int k;
				do
				{
					k = new Random().nextInt(moduleNum);
				} while (tempModuleEqNum[k] <= 0);

				chromosomeByInt.add(k);
				tempModuleEqNum[k]--;
			}

			// 설비 배치 순서가 숫자 형태로 되어 있는 것을 이름으로 Mapping 하는 작업.
			chromosomeByName = eqMappingToName(chromosomeByInt, chromosomeByName);

			// populatin 생성.
			/*임대은 review
			 * chromosomeByInt는 EQ가 어느 모듈에서 나올지를 갖고 있다.
			 * chromosomeByName은 EQ의 순서를 이름으로 갖고 있다.
			 * propertiesByInt는 숫자가 어떤 모듈인지를 갖고 있다.
			 * */
			population = new MicroOrganism(chromosomeByInt, chromosomeByName, propertiesByInt);

			// population의 EQ를 순서에 맞게 Room에 배치함.
//			eqArrange(population);

			// Population의 EQ 적합도(이동동선, CrossOver)를 계산함
			calcFitness(population, room, room.isLastRoom());

			lstPopulations.add(population);

		}// End For (int i = 0; i < population; i++)

//		System.out.println("초기화 완료");
	}
//	/**
//	 * 
//	 * @author : jwon.cho
//	 * @version : 2011.05.18
//	 * @date : 2011.05.18
//	 * @변경이력 :
//	 * @Method 설명 : 초기해가 주어진 경우(이미 계산 한 번 한 이후 Micro Popuation Size만큼 이미 해를 구한 이후)
//	 * 
//	 */
//	private void initialize(ArrayList<MicroOrganism> initPopulation)
//	{
//		MicroOrganism population;
//		
//		for (int i = 0; i < CDataSet.MICRO_POPULATIONS_SIZE; i++)
//		{
//			// populatin 생성.
//			population = new MicroOrganism(initPopulation.get(i).getChromosomeByInt(), initPopulation.get(i).getChromosomeByName(), propertiesByInt);
//
//			// population의 EQ를 순서에 맞게 Room에 배치함.
//			eqArrange(population);
//
//			// Population의 EQ 적합도(이동동선, CrossOver)를 계산함
//			calcFitness(population, room, room.isLastRoom());
//
//			lstPopulations.add(population);
//
//		}// End For (int i = 0; i < population; i++)
//
////		System.out.println("초기화 완료");
//	}

	
	/**
	 * Zone의 시작점을 기준으로 zone을 대칭이동한다.
	 *  y=x에 대해 대칭이동 / 기준점(left, top)
	 */
	public void convertZoneCoord() {
		for (int i = 0; i < dataSet.getM_lstZone().size(); i++) {
			CZone zone = dataSet.getM_lstZone().get(i);
			if(zone.getType().equals("가로")){
				double top, width, height;
				width = zone.getWidth();
				height = zone.getHeight();
				zone.setWidth(height);
				zone.setHeight(width);
			}
		}
	}
	/**
	 * fixedEq 돌리기
	 */
	public void convertFixedEqCoord() {
		double zoneLeft, zoneTop, zoneHeight, zoneWidth, datumPointX, datumPointY, fixedEqLeft, fixedEqTop, fixedEqWidth, fixedEqHeight;
		CZone zone = new CZone();
		
		for (int i = 0; i < dataSet.getM_lstFixedEq().size(); i++){
			EQ fixedEq = dataSet.getM_lstFixedEq().get(i);
			
			for (int m = 0; m < dataSet.getM_lstZone().size(); m++) {
				if(dataSet.getM_lstZone().get(m).getZoneIndex() == fixedEq.getZoneIndex()) 
					zone = dataSet.getM_lstZone().get(m);
			}
			if(zone.getType().equals("가로")){
				zoneTop = zone.getTop();
				zoneLeft = zone.getLeft();
				zoneHeight = zone.getHeight();
				zoneWidth = zone.getWidth();
				fixedEqLeft =  fixedEq.getCoordLeft();
				fixedEqTop = fixedEq.getCoordTop();
				fixedEqWidth = fixedEq.getCoordWidth();
				
				fixedEqHeight = fixedEq.getCoordLength();
				
				datumPointX = zoneLeft;
				datumPointY = zoneTop;
				fixedEq.setCoordLeft(fixedEqTop-datumPointY+datumPointX);
				fixedEq.setCoordTop(fixedEqLeft-datumPointX+datumPointY);
				fixedEq.setCoordLength(fixedEqWidth);
				fixedEq.setCoordWidth(fixedEqHeight);
			}
		}
	}
	/**
	 * room 돌리듯 eq 돌리기
	 */
	public void convertEQCoord() {
		double zoneLeft, zoneTop, zoneHeight, zoneWidth, datumPointX, datumPointY, eqLeft, eqTop, eqWidth, eqHeight;
		for (int i = 0; i < resultSet.getM_bestRoomOrder().getChromosomeByName().size(); i++)
		{
			CRoom room = resultSet.getM_bestRoomOrder().getChromosomeByName().get(i);
//			if(room.????.size()>0){ // room이 너무 좁아 bay가 배정되지 않을 수 있음 jwon.cho 2010-12-16
//				for (int j = 0; j < room.getM_lstBayResult().get(0).getM_lstBay().size(); j++)
//				{
//					CBay bay = room.getM_lstBaynewResult().get(0).getM_lstBay().get(j);
//					for (int k = 0; k < bay.getListEq().size(); k++)
			
			if(room.getM_lstBaynewResult().getM_lstBay().size()>0){ // room이 너무 좁아 bay가 배정되지 않을 수 있음 jwon.cho 2010-12-16
			for (int j = 0; j < room.getM_lstBaynewResult().getM_lstBay().size(); j++)
			{
				CBay bay = room.getM_lstBaynewResult().getM_lstBay().get(j);
				for (int k = 0; k < bay.getListEq().size(); k++)
					{
						EQ eq = bay.getListEq().get(k);
						CZone zone = new CZone();
						for (int m = 0; m < dataSet.getM_lstZone().size(); m++) {
							if(dataSet.getM_lstZone().get(m).getZoneIndex()==bay.getZone()) 
								//eq의 zone을 사용했더니, zone이 바뀌었을 때 첫 EQ는 이전 zoneIndex를 가지고 있어서 이상함
								zone = dataSet.getM_lstZone().get(m);
						}
						if(zone.getType().equals("가로")){
							zoneTop = zone.getTop();
							zoneLeft = zone.getLeft();
							zoneHeight = zone.getHeight();
							zoneWidth = zone.getWidth();
							eqLeft =  eq.getCoordLeft();
							eqTop = eq.getCoordTop();
							eqWidth = eq.getCoordWidth();
							eqHeight = eq.getCoordLength();
							
							datumPointX = zoneLeft;
							datumPointY = zoneTop;
							eq.setCoordLeft(eqTop-datumPointY+datumPointX);
							eq.setCoordTop(eqLeft-datumPointX+datumPointY);
							eq.setCoordLength(eqWidth);
							eq.setCoordWidth(eqHeight);
						}
					}
				}
			}
		}
	}
	private Renewal_MicroOrganism convertEQCoord(Renewal_MicroOrganism population) {
		ArrayList<EQ> EQList = population.getChromosomeByName();
		double zoneLeft, zoneTop, zoneHeight, zoneWidth, datumPointX, datumPointY, eqLeft, eqTop, eqWidth, eqHeight;
		for (int k = 0; k < EQList.size(); k++)
		{
			EQ eq = EQList.get(k);
			CZone zone = new CZone();
			zone = getZone(eq);
			
			if(zone.getType().equals("가로")){
				zoneTop = zone.getTop();
				zoneLeft = zone.getLeft();
				zoneHeight = zone.getHeight();
				zoneWidth = zone.getWidth();
				eqLeft =  eq.getCoordLeft();
				eqTop = eq.getCoordTop();
				eqWidth = eq.getCoordWidth();
				eqHeight = eq.getCoordLength();
	
				datumPointX = zoneLeft;
				datumPointY = zoneTop;
				eq.setCoordLeft(eqTop-datumPointY+datumPointX);
				eq.setCoordTop(eqLeft-datumPointX+datumPointY);
				eq.setCoordLength(eqWidth);
				eq.setCoordWidth(eqHeight);
			}
			
		}
		return population;
	}
	private CZone getZone(EQ eq) {
		CZone zone = null;
		for (int m = 0; m < dataSet.getM_lstZone().size(); m++) {
			if(dataSet.getM_lstZone().get(m).getZoneIndex()==eq.getZoneIndex()) 
				//eq의 zone을 사용했더니, zone이 바뀌었을 때 첫 EQ는 이전 zoneIndex를 가지고 있어서 이상함
				zone = dataSet.getM_lstZone().get(m);
		}
		return zone;
	}

	/**
	 * 
	 * @author : kyveri.kim
	 * @version : 2010. 07. 21 오후 5:24:32
	 * @date : 2010. 07. 21
	 * @param population
	 * @return
	 * @변경이력 :
	 * @Method 설명 : 설비에 의한 점유면적 최소화. Zone의 면적에서 마지막 Bay의 위치를 보고 계산한다.
	 * zoneRemainSize를 매번 더해서 너무 큰 값이 나왔음.  마지막에 한 번만 더해야 함. jwon.cho
	 */
	private double calFitnessByArea(MicroOrganism population, boolean isLastRoom)
	{
		
		int bayIndex = population.getM_lstBay().size();
		CBay bay = population.getM_lstBay().get(bayIndex-1);
		int zoneIndex = bay.getZone();
		boolean isBayUpperDirection = bay.isM_isBayUpperdirection();
		boolean isEqUpperDirection = bay.isM_isEQUpperDirection();
		
		double zoneWidth = dataSet.getM_lstZone().get(zoneIndex).getWidth(); 
		double zoneHeight = dataSet.getM_lstZone().get(zoneIndex).getHeight();
		double zoneTop = dataSet.getM_lstZone().get(zoneIndex).getTop();
		double zoneRemainHeight=0.0f;
		double bayRemainSize = 0.0f;
		double bayHeight = 0.0f;
		double zoneRemainSize=0.0f;
		double remainSize=0.0f;
		double bayTop = bay.getTop();
		
		double unusedArea=0.0f;
		
		if(isBayUpperDirection) {
			if(isEqUpperDirection)
			{
				// Bay 설비들의 평균 높이
				int eqCnt = bay.getListEq().size();
				for (int i = 0; i < eqCnt; i++)
				{
					bayHeight = bayHeight + bay.getListEq().get(i).getCoordLength();
				}
				bayHeight = bayHeight/(double)eqCnt;
				
				// Bay에서 설비 점유하고 남은 공간
				bayRemainSize = bayHeight * bay.getRemainWidth();
				
				// zone에서 남은 공간의 높이
				zoneRemainHeight = zoneTop + zoneHeight - bayTop;	
				
				// zone에서 남은 공간
				zoneRemainSize = zoneRemainHeight * zoneWidth;
				
				// 전체 남은 공간
				remainSize = bayRemainSize + zoneRemainSize;
				if(isLastRoom) unusedArea = bayRemainSize + zoneRemainSize;
				else unusedArea = bayRemainSize;
				 
//				System.out.println("1");			
//				System.out.println("bayHeight: " + bayHeight + "	bay.getRemainWidth(): " + bay.getRemainWidth() + "	zoneTop: " + zoneTop + 
//						"	zoneHeight: " + zoneHeight + "	bayTop: " + bayTop + "zoneRemainHeight: " + zoneRemainHeight + "	zoneWidth: " + zoneWidth);							
//System.out.println("isBayUpperDirection: " + isBayUpperDirection + "	isEqUpperDirection: " + isEqUpperDirection + "	bayRemainSize: " + bayRemainSize/10000 + 
//"	zoneRemainSize: " + zoneRemainSize/10000 + "	remainSize: " + remainSize/10000);				
			}else 
			{
				// Bay 설비에서 높이가 가장 큰 설비의 높이
				int eqCnt = bay.getListEq().size();
				for (int i = 0; i < eqCnt; i++)
				{
					if (bayHeight < bay.getListEq().get(i).getCoordLength())
						bayHeight = bay.getListEq().get(i).getCoordLength();
				}
				
				// Bay에서 설비 점유하고 남은 공간
				bayRemainSize = bayHeight * bay.getRemainWidth();
				
				// zone에서 남은 공간의 높이
				zoneRemainHeight = zoneTop + zoneHeight - (bayTop + bayHeight);	
				
				// zone에서 남은 공간
				zoneRemainSize = zoneRemainHeight * zoneWidth;
				
				// 전체 남은 공간
				remainSize = bayRemainSize + zoneRemainSize;
				if(isLastRoom) unusedArea = bayRemainSize + zoneRemainSize;
				else unusedArea = bayRemainSize;
//				System.out.println("2");			
//				System.out.println("bayHeight: " + bayHeight + "	bay.getRemainWidth(): " + bay.getRemainWidth() + "	zoneTop: " + zoneTop + 
//						"	zoneHeight: " + zoneHeight + "	bayTop: " + bayTop + "zoneRemainHeight: " + zoneRemainHeight + "	zoneWidth: " + zoneWidth);
//				System.out.println("isBayUpperDirection: " + isBayUpperDirection + "	isEqUpperDirection: " + isEqUpperDirection + "	bayRemainSize: " + bayRemainSize/10000 + 
//						"	zoneRemainSize: " + zoneRemainSize/10000 + "	remainSize: " + remainSize/10000);				
			}
		} else 
		{
			if(isEqUpperDirection)
			{
				// Bay 설비에서 높이가 가장 큰 설비의 높이
				int eqCnt = bay.getListEq().size();
				for (int i = 0; i < eqCnt; i++)
				{
					if (bayHeight < bay.getListEq().get(i).getCoordLength())
						bayHeight = bay.getListEq().get(i).getCoordLength();
				}
				
				// Bay에서 설비 점유하고 남은 공간
				bayRemainSize = bayHeight * bay.getRemainWidth();
				
				// zone에서 남은 공간의 높이
				zoneRemainHeight = (bayTop - bayHeight) - zoneTop;	
				
				// zone에서 남은 공간
				zoneRemainSize = zoneRemainHeight * zoneWidth;
				
				// 전체 남은 공간
				remainSize = bayRemainSize + zoneRemainSize;
				if(isLastRoom) unusedArea = bayRemainSize + zoneRemainSize;
				else unusedArea = bayRemainSize;
//				System.out.println("3");			
//				System.out.println("bayHeight: " + bayHeight + "	bay.getRemainWidth(): " + bay.getRemainWidth() + "	zoneTop: " + zoneTop + 
//						"	zoneHeight: " + zoneHeight + "	bayTop: " + bayTop + "zoneRemainHeight: " + zoneRemainHeight + "	zoneWidth: " + zoneWidth);
//				System.out.println("isBayUpperDirection: " + isBayUpperDirection + "	isEqUpperDirection: " + isEqUpperDirection + "	bayRemainSize: " + bayRemainSize/10000 + 
//						"	zoneRemainSize: " + zoneRemainSize/10000 + "	remainSize: " + remainSize/10000);				
				
			}else 
			{
				// Bay 설비들의 평균 높이
				int eqCnt = bay.getListEq().size();
				for (int i = 0; i < eqCnt; i++)
				{
					bayHeight = bayHeight + bay.getListEq().get(i).getCoordLength();
				}
				bayHeight = bayHeight/(double)eqCnt;
				
				// Bay에서 설비 점유하고 남은 공간
				bayRemainSize = bayHeight * bay.getRemainWidth();
				
				// zone에서 남은 공간의 높이
				zoneRemainHeight = bayTop - zoneTop;	
				
				// zone에서 남은 공간
				zoneRemainSize = zoneRemainHeight * zoneWidth;
				
				// 전체 남은 공간
				remainSize = bayRemainSize + zoneRemainSize;
				if(isLastRoom) unusedArea = bayRemainSize + zoneRemainSize;
				else unusedArea = bayRemainSize;
//				System.out.println("4");			
//				System.out.println("bayHeight: " + bayHeight + "	bay.getRemainWidth(): " + bay.getRemainWidth() + "	zoneTop: " + zoneTop + 
//						"	zoneHeight: " + zoneHeight + "	bayTop: " + bayTop + "zoneRemainHeight: " + zoneRemainHeight + "	zoneWidth: " + zoneWidth);
//				System.out.println("isBayUpperDirection: " + isBayUpperDirection + "	isEqUpperDirection: " + isEqUpperDirection + "	bayRemainSize: " + bayRemainSize/10000 + 
//						"	zoneRemainSize: " + zoneRemainSize/10000 + "	remainSize: " + remainSize/10000);				
			}
		}
		population.setUnusedArea(unusedArea/1000000);
		return remainSize/1000000;
	}
	
	// 새로운 거리 fitness 계산 함수
	public Renewal_MicroOrganism calFitnessByDistance_renew(Renewal_MicroOrganism chromosome) {
		// hashmap으로 베이의 짝을 관리
		// 만약 오류가 발생하면, 해당 row에서 베이의 수가 홀수이어서 발생하는 오류
		Map <Integer,Integer> bay_couple=new HashMap<Integer,Integer>();
		for(int i=0;i<chromosome.m_lstBay.size()-1;i+=2) {
			if (chromosome.m_lstBay.get(i).getZone()==chromosome.m_lstBay.get(i+1).getZone()) {
				bay_couple.put(i,i+1);
				bay_couple.put(i+1,i);
			}
		}
				
		// chromosome내 각 베이를 읽는다.
		
		double total_dist=0;
		
		for(int i=0;i<chromosome.m_lstBay.size();i++) {
			CBay frombay=chromosome.m_lstBay.get(i);
			// 각 베이의 eq를 읽는다.
			for(int j=0;j<chromosome.m_lstBay.get(i).getListEq().size();j++) {
				// fromEQ로 설정
				EQ fromEQ=chromosome.m_lstBay.get(i).getListEq().get(j);
				double each_eq_dist=0.0;
				// toEQ를 읽는다.
				for(int k=0;k<chromosome.m_lstBay.size();k++) {
					// tobay로 설정
					CBay tobay=chromosome.m_lstBay.get(k);
					for(int l=0;l<chromosome.m_lstBay.get(k).getListEq().size();l++) {						
						EQ toEQ=chromosome.m_lstBay.get(k).getListEq().get(l);
						double freq=0;
						String fromEQName=fromEQ.getDeviceName();
						String toEQName=toEQ.getDeviceName();
						String key = fromEQName + "$" + toEQName;
						if(null == dataSet.getM_htFromTo_EQ().get(key)) {}
						else freq = Float.parseFloat(dataSet.getM_htFromTo_EQ().get(key).toString());
						if (fromEQ!=toEQ && freq>0) {
							each_eq_dist+=freq*distance_fitness(fromEQ,toEQ,frombay,tobay,i,k,bay_couple,chromosome);
						}
						else {
							each_eq_dist+=0;
						}
					}
				}
				total_dist+=each_eq_dist;
			}
		}
		
		chromosome.set_fitness_value(total_dist);
		
		return chromosome;
	}
	
	public double distance_fitness(EQ fromEQ, EQ toEQ, CBay frombay, CBay tobay, int i, int k, Map<Integer,Integer> bay_couple, Renewal_MicroOrganism chromosome) {
		double eqtozone=0;
		double zonetozone=0;
		double zonetoeq=0;
		// 동일 베이내 좌 또는 우에 같이 있는 경우
		if (frombay==tobay) {
			// 베이가 아랫 윗 방향인 경우
			if(frombay.isM_isBayUpperdirection()) {
				//시계 방향
				if(fromEQ.getCoordTop()<toEQ.getCoordTop()) {						
					eqtozone=Math.abs((fromEQ.getCoordTop()+fromEQ.getCoordLength()/2)-(toEQ.getCoordTop()+toEQ.getCoordLength()/2));
				}
				// 시계 반대방향 (짝 row로 이동)
				else {
					// 베이가 아래쪽에 있는 경우
					if(frombay.isM_isBayUpperdirection()) {
						// 1. 시작 설비에서 central loop까지 가는 거리
						eqtozone+=(frombay.getTop()+frombay.getWidth())-(fromEQ.getCoordTop()+fromEQ.getCoordLength()/2);
						// 2. central loop내 이동거리
						// 현재 row를 부름
						CZone now_row=dataSet.getM_lstZone().get(frombay.getZone());
						// 반대편 row를 부름. 베이가 아래에 배치되어있기 때문에 index +1
						CZone opposite_row=dataSet.getM_lstZone().get(frombay.getZone()-1);
						// central loop 왼쪽 이동거리
						zonetozone+=frombay.getLeft()-Math.min(now_row.getLeft(),opposite_row.getLeft());
						// central loop 위로 이동하는 거리
						zonetozone+=opposite_row.getTop()-(now_row.getTop()+now_row.getHeight());
						// central loop 오른쪽으로 이동하는 거리
						zonetozone+=Math.max(now_row.getLeft()+now_row.getWidth(),opposite_row.getLeft()+opposite_row.getWidth())-Math.min(now_row.getLeft(),opposite_row.getLeft());
						// central loop 내려가는 거리
						zonetozone+=opposite_row.getTop()-(now_row.getTop()+now_row.getHeight());
						// central loop 도착 베이의 짝까지 이동하는 거리
						// 도착 베이의 짝베이
						CBay destinationbay=chromosome.getM_lstBay().get(bay_couple.get(k));
						zonetozone+=Math.max(now_row.getLeft()+now_row.getWidth(),opposite_row.getLeft()+opposite_row.getWidth())-destinationbay.getLeft();
						// 3. central loop에서 설비까지 이동거리
						// 내려가는거리
						zonetoeq+=destinationbay.getWidth();
						// 왼쪽 이동
						zonetoeq+=tobay.getLeft()-destinationbay.getLeft();
						// 설비까지 이동
						zonetoeq+=toEQ.getCoordTop()+toEQ.getCoordLength()/2-tobay.getTop();
					}
					// 베이가 위쪽에 있는 경우
					else {
						// 1. 시작 설비에서 central loop까지 가는 거리
						// 시작베이의 짝베이 선언
						CBay startbay=chromosome.getM_lstBay().get(bay_couple.get(i));
						// 올라가는 거리
						eqtozone+=(frombay.getTop()+frombay.getWidth())-(fromEQ.getCoordTop()+fromEQ.getCoordLength()/2);
						// 오른쪽 이동
						eqtozone+=frombay.getLeft()-startbay.getLeft();
						// 내려가는 거리
						eqtozone+=startbay.getWidth();							
						// 2. central loop내 이동거리
						// 현재 row를 부름
						CZone now_row=dataSet.getM_lstZone().get(frombay.getZone());
						// 반대편 row를 부름. 베이가 위에 배치되어있기 때문에 index -1
						CZone opposite_row=dataSet.getM_lstZone().get(frombay.getZone()+1);
						// central loop 오른쪽 이동거리
						zonetozone+=Math.max(now_row.getLeft()+now_row.getWidth(),opposite_row.getLeft()+opposite_row.getWidth())-startbay.getLeft();
						// central loop 아래로 이동하는 거리
						zonetozone+=now_row.getTop()-(opposite_row.getTop()+opposite_row.getHeight());
						// central loop 왼쪽으로 이동하는 거리
						zonetozone+=Math.max(now_row.getLeft()+now_row.getWidth(),opposite_row.getLeft()+opposite_row.getWidth())-Math.min(now_row.getLeft(),opposite_row.getLeft());
						// central loop 올라가는 거리
						zonetozone+=now_row.getTop()-(opposite_row.getTop()+opposite_row.getHeight());
						// central loop 도착 베이까지 이동하는 거리
						zonetozone+=tobay.getLeft()-Math.min(now_row.getLeft(),opposite_row.getLeft());
						// 3. central loop에서 설비까지 이동거리
						// 설비까지 올라가는 이동거리
						zonetoeq+=toEQ.getCoordTop()+toEQ.getCoordLength()/2-tobay.getTop();
					}
				}
			}
			// 베이가 윗 방향인 경우 
			else {
				// 시계 방향
				if(fromEQ.getCoordTop()<toEQ.getCoordTop()) {
					eqtozone=Math.abs((fromEQ.getCoordTop()+fromEQ.getCoordLength()/2)-(toEQ.getCoordTop()+toEQ.getCoordLength()/2));
				}
				// 시계 반대 방향  (짝 로우로 이동)
				else {
					// 베이가 아래쪽에 있는 경우
					if(frombay.isM_isBayUpperdirection()) {
						// 1. 시작 설비에서 central loop까지 가는 거리
						// 시작베이의 짝 베이 선언
						CBay startbay=chromosome.getM_lstBay().get(bay_couple.get(i));
						// 내려가는 거리
						eqtozone+=(fromEQ.getCoordTop()+fromEQ.getCoordLength()/2)-frombay.getTop();
						// 짝베이로 이동거리
						eqtozone+=frombay.getLeft()-startbay.getLeft();
						// 짝베이에서 zone까지 올라가는 거리
						eqtozone+=startbay.getWidth();
						// 2. central loop내 이동거리
						// 현재 row를 부름
						CZone now_row=dataSet.getM_lstZone().get(frombay.getZone());
						// 반대편 row를 부름. 베이가 아래에 배치되어있기 때문에 index +1
						CZone opposite_row=dataSet.getM_lstZone().get(frombay.getZone()-1);
						// central loop 왼쪽 이동거리
						zonetozone+=startbay.getLeft()-Math.min(now_row.getLeft(),opposite_row.getLeft());
						// central loop 위로 이동하는 거리
						zonetozone+=opposite_row.getTop()-(now_row.getTop()+now_row.getHeight());
						// central loop 오른쪽으로 이동하는 거리
						zonetozone+=Math.max(now_row.getLeft()+now_row.getWidth(),opposite_row.getLeft()+opposite_row.getWidth())-Math.min(now_row.getLeft(),opposite_row.getLeft());
						// central loop 내려가는 거리
						zonetozone+=opposite_row.getTop()-(now_row.getTop()+now_row.getHeight());
						// central loop 도착 베이의 짝까지 이동하는 거리
						zonetozone+=Math.max(now_row.getLeft()+now_row.getWidth(),opposite_row.getLeft()+opposite_row.getWidth())-tobay.getLeft();
						// 3. central loop에서 설비까지 이동거리
						// 내려가는거리
						zonetoeq+=(tobay.getTop()+tobay.getWidth())-(toEQ.getCoordTop()+toEQ.getCoordLength()/2);
					}
					// 베이가 위쪽에 있는 경우
					else {
						// 1. 시작 설비에서 central loop까지 가는 거리							
						// 내려가는 거리
						eqtozone+=(fromEQ.getCoordTop()+fromEQ.getCoordLength()/2)-frombay.getTop();					
						// 2. central loop내 이동거리
						// 현재 row를 부름
						CZone now_row=dataSet.getM_lstZone().get(frombay.getZone());
						// 반대편 row를 부름. 베이가 위에 배치되어있기 때문에 index -1
						CZone opposite_row=dataSet.getM_lstZone().get(frombay.getZone()+1);
						// central loop 오른쪽 이동거리
						zonetozone+=Math.max(now_row.getLeft()+now_row.getWidth(),opposite_row.getLeft()+opposite_row.getWidth())-frombay.getLeft();
						// central loop 아래로 이동하는 거리
						zonetozone+=now_row.getTop()-(opposite_row.getTop()+opposite_row.getHeight());							
						// central loop 왼쪽으로 이동하는 거리
						zonetozone+=Math.max(now_row.getLeft()+now_row.getWidth(),opposite_row.getLeft()+opposite_row.getWidth())-Math.min(now_row.getLeft(),opposite_row.getLeft());
						// central loop 올라가는 거리
						zonetozone+=now_row.getTop()-(opposite_row.getTop()+opposite_row.getHeight());
						// central loop 도착 베이까지 이동하는 거리
						CBay destinationbay=chromosome.getM_lstBay().get(bay_couple.get(k));
						zonetozone+=destinationbay.getLeft()-Math.min(now_row.getLeft(),opposite_row.getLeft());							
						// 3. central loop에서 설비까지 이동거리
						// 설비까지 올라가는 이동거리
						zonetoeq+=destinationbay.getWidth();
						zonetoeq+=tobay.getLeft()-destinationbay.getLeft();
						zonetoeq+=(tobay.getTop()+tobay.getWidth())-(toEQ.getCoordTop()+toEQ.getCoordLength()/2);
					}
				}
			}
		}
		
		// 동일 베이내 좌, 우 각각 존재하는 경우
		else if (bay_couple.get(i)==k) {
			// 베이가 아래인 경우 (맨 아래 로우)
			if(frombay.isM_isBayUpperdirection()) {
				// 반대 로우로 안 넘어가는 경우
				if(i>k) {
					eqtozone+=fromEQ.getCoordTop()+fromEQ.getCoordLength()/2-frombay.getTop();
					eqtozone+=frombay.getLeft()-tobay.getLeft();
					eqtozone+=toEQ.getCoordTop()+toEQ.getCoordLength()/2-tobay.getTop();						
				}
				// 반대 로우로 넘어가는 경우
				else {						
					// 1. 시작 설비에서 central loop까지 가는 거리
					eqtozone+=(frombay.getTop()+frombay.getWidth())-(fromEQ.getCoordTop()+fromEQ.getCoordLength()/2);
					// 2. central loop내 이동거리
					// 현재 row를 부름
					CZone now_row=dataSet.getM_lstZone().get(frombay.getZone());
					// 반대편 row를 부름. 베이가 아래에 배치되어있기 때문에 index +1
					CZone opposite_row=dataSet.getM_lstZone().get(frombay.getZone()-1);
					// central loop 왼쪽 이동거리
					zonetozone+=frombay.getLeft()-Math.min(now_row.getLeft(),opposite_row.getLeft());
					// central loop 위로 이동하는 거리
					zonetozone+=opposite_row.getTop()-(now_row.getTop()+now_row.getHeight());
					// central loop 오른쪽으로 이동하는 거리
					zonetozone+=Math.max(now_row.getLeft()+now_row.getWidth(),opposite_row.getLeft()+opposite_row.getWidth())-Math.min(now_row.getLeft(),opposite_row.getLeft());
					// central loop 내려가는 거리
					zonetozone+=opposite_row.getTop()-(now_row.getTop()+now_row.getHeight());						// 
					// central loop 도착 베이까지 이동하는 거리 						
					zonetozone+=Math.max(now_row.getLeft()+now_row.getWidth(),opposite_row.getLeft()+opposite_row.getWidth())-tobay.getLeft();
					// 3. central loop에서 설비까지 이동거리						
					// 설비까지 이동
					zonetoeq+=(tobay.getTop()+tobay.getWidth())-(toEQ.getCoordTop()+toEQ.getCoordLength()/2);
				}
			}
			// 베이가 위에 있는 경우 (아래에서 두번째 로우)
			else {
				// 반대 로우로 안 넘어가는 경우
				if(i<k) {
					eqtozone+=(frombay.getTop()+frombay.getWidth())-(fromEQ.getCoordTop()+fromEQ.getCoordLength()/2);
					eqtozone+=tobay.getLeft()-frombay.getLeft();
					eqtozone+=(tobay.getTop()+tobay.getWidth())-(toEQ.getCoordTop()+toEQ.getCoordLength()/2);						
				}
				// 반대 로우로 넘어가는 경우
				else {
					// 1. 시작 설비에서 central loop까지 가는 거리							
					// 내려가는 거리
					eqtozone+=(fromEQ.getCoordTop()+fromEQ.getCoordLength()/2)-(frombay.getTop()+frombay.getWidth());					
					// 2. central loop내 이동거리
					// 현재 row를 부름
					CZone now_row=dataSet.getM_lstZone().get(frombay.getZone());
					// 반대편 row를 부름. 베이가 위에 배치되어있기 때문에 index -1
					CZone opposite_row=dataSet.getM_lstZone().get(frombay.getZone()+1);
					// central loop 오른쪽 이동거리
					zonetozone+=Math.max(now_row.getLeft()+now_row.getWidth(),opposite_row.getLeft()+opposite_row.getWidth())-frombay.getLeft();
					// central loop 아래로 이동하는 거리
					zonetozone+=now_row.getTop()-(opposite_row.getTop()+opposite_row.getHeight());							
					// central loop 왼쪽으로 이동하는 거리
					zonetozone+=Math.max(now_row.getLeft()+now_row.getWidth(),opposite_row.getLeft()+opposite_row.getWidth())-Math.min(now_row.getLeft(),opposite_row.getLeft());
					// central loop 올라가는 거리
					zonetozone+=now_row.getTop()-(opposite_row.getTop()+opposite_row.getHeight());
					// central loop 도착 베이까지 이동하는 거리						
					zonetozone+=tobay.getLeft()-Math.min(now_row.getLeft(),opposite_row.getLeft());							
					// 3. central loop에서 설비까지 이동거리
					// 설비까지 올라가는 이동거리
					zonetoeq+=(toEQ.getCoordTop()+toEQ.getCoordLength()/2)-tobay.getTop();	
				}				
			}
		}

		// 서로 다른 베이에 존재하는 경우
		else {
			// 1. eqtozone
			// central loop에서 출발하는 x
			double starting_x_central_loop;
			// 출발 설비의 베이가 아래있고, 출발 설비가 위를 향하는 경우
			if(frombay.isM_isBayUpperdirection() && frombay.isM_isEQUpperDirection()) {
				eqtozone+=(frombay.getTop()+frombay.getWidth())-(fromEQ.getCoordTop()+fromEQ.getCoordLength()/2);
				starting_x_central_loop=frombay.getLeft();				
			}
			// 출발 설비의 베이가 아래있고, 출발 설비가 아래를 향하는 경우 
			else if(frombay.isM_isBayUpperdirection() && !frombay.isM_isEQUpperDirection()) {
				// 시작베이의 짝 베이 선언
				CBay startbay=chromosome.getM_lstBay().get(bay_couple.get(i));
				// 내려가는 거리
				eqtozone+=(fromEQ.getCoordTop()+fromEQ.getCoordLength()/2)-frombay.getTop();
				// 짝베이로 이동거리
				eqtozone+=frombay.getLeft()-startbay.getLeft();
				// 짝베이에서 zone까지 올라가는 거리
				eqtozone+=startbay.getWidth();		
				starting_x_central_loop=startbay.getLeft();				
			}
			// 출발 설비의 베이가 위에있고, 출발 설비가 위를 향하는 경우 
			else if(!frombay.isM_isBayUpperdirection() && frombay.isM_isEQUpperDirection()) {				
				// 시작베이의 짝베이 선언
				CBay startbay=chromosome.getM_lstBay().get(bay_couple.get(i));
				// 올라가는 거리
				eqtozone+=(frombay.getTop()+frombay.getWidth())-(fromEQ.getCoordTop()+fromEQ.getCoordLength()/2);
				// 오른쪽 이동
				eqtozone+=frombay.getLeft()-startbay.getLeft();
				// 내려가는 거리
				eqtozone+=startbay.getWidth();
				starting_x_central_loop=startbay.getLeft();
			}
			// 출발 설비의 베이가 위에있고, 출발 설비가 아래를 향하는 경우
			else {
				eqtozone+=(fromEQ.getCoordTop()+fromEQ.getCoordLength()/2)-frombay.getTop();
				starting_x_central_loop=frombay.getLeft();
			}			

			// 3. zonetoeq
			// central loop에서 출발하는 x
			double ending_x_central_loop;			
			// 도착 설비의 베이가 아래있고, 도착 설비가 위를 향하는 경우
			if(tobay.isM_isBayUpperdirection() && tobay.isM_isEQUpperDirection()) {
				CBay destinationbay=chromosome.getM_lstBay().get(bay_couple.get(k));
				zonetoeq+=destinationbay.getWidth();
				zonetoeq+=destinationbay.getLeft()-tobay.getLeft();
				zonetoeq+=(toEQ.getCoordTop()+toEQ.getCoordLength()/2)-tobay.getTop();
				ending_x_central_loop=destinationbay.getLeft();
			}
			// 도착 설비의 베이가 아래있고, 도착 설비가 아래를 향하는 경우
			else if(tobay.isM_isBayUpperdirection() && !tobay.isM_isEQUpperDirection()) {
				zonetoeq+=(tobay.getTop()+tobay.getWidth())-(toEQ.getCoordTop()+toEQ.getCoordLength()/2);
				ending_x_central_loop=tobay.getLeft();				
			}
			// 도착 설비의 베이가 위에있고, 도착 설비가 위를 향하는 경우
			else if(!tobay.isM_isBayUpperdirection() && tobay.isM_isEQUpperDirection()) {
				zonetoeq+=(toEQ.getCoordTop()+toEQ.getCoordLength()/2)-tobay.getTop();
				ending_x_central_loop=tobay.getLeft();
				
			}
			// 도착 설비의 베이가 위에있고, 도착 설비가 아래를 향하는 경우
			else {
				CBay destinationbay=chromosome.getM_lstBay().get(bay_couple.get(k));
				zonetoeq+=destinationbay.getWidth();
				zonetoeq+=tobay.getLeft()-destinationbay.getLeft();
				zonetoeq+=(tobay.getTop()-tobay.getWidth())-(toEQ.getCoordTop()+toEQ.getCoordLength()/2);
				ending_x_central_loop=destinationbay.getLeft();
			}
			
			// 2. zonetozone
			// 같은 존에 시작, 도착베이 위치
			if(tobay.getZone()==frombay.getZone()) {
				// 반대 존으로 이동하지 않음
				if(i>k) {
					zonetozone=Math.abs(starting_x_central_loop-ending_x_central_loop);
				}
				// 반대 존으로 이동
				else {
					// 베이가 아래에 위치한 경우
					if(tobay.isM_isBayUpperdirection()) {
						// 현재 row를 부름
						CZone now_row=dataSet.getM_lstZone().get(frombay.getZone());
						// 반대편 row를 부름. 베이가 아래에 배치되어있기 때문에 index +1
						CZone opposite_row=dataSet.getM_lstZone().get(frombay.getZone()-1);
						zonetozone+=starting_x_central_loop-Math.min(now_row.getLeft(), opposite_row.getLeft());
						// 로우간 이동의 경우 왕복이기 때문에 2배를 해서 더해줌
						zonetozone+=(opposite_row.getTop()-(now_row.getTop()+now_row.getHeight()))*2;
						zonetozone+=Math.max(now_row.getLeft()+now_row.getWidth(),opposite_row.getLeft()+opposite_row.getWidth())-Math.min(now_row.getLeft(), opposite_row.getLeft());
						zonetozone+=Math.max(now_row.getLeft()+now_row.getWidth(),opposite_row.getLeft()+opposite_row.getWidth())-ending_x_central_loop;
					}
					// 베이가 위에 위치한 경우
					else {
						// 현재 row를 부름
						CZone now_row=dataSet.getM_lstZone().get(frombay.getZone());
						// 반대편 row를 부름. 베이가 위에 배치되어있기 때문에 index -1
						CZone opposite_row=dataSet.getM_lstZone().get(frombay.getZone()+1);
						zonetozone+=Math.max(now_row.getLeft()+now_row.getWidth(),opposite_row.getLeft()+opposite_row.getWidth())-starting_x_central_loop;
						// 로우간 이동의 경우 왕복이기 때문에 2배를 해서 더해줌
						zonetozone+=(opposite_row.getTop()-(now_row.getTop()+now_row.getHeight()))*2;
						zonetozone+=Math.max(now_row.getLeft()+now_row.getWidth(),opposite_row.getLeft()+opposite_row.getWidth())-Math.min(now_row.getLeft(), opposite_row.getLeft());
						zonetozone+=ending_x_central_loop-Math.min(now_row.getLeft(), opposite_row.getLeft());
					}
				}
			}
			// 다른 존에 시작, 도착베이 위치
			else {
				// 현재 로우를 부름
				CZone now_row=dataSet.getM_lstZone().get(frombay.getZone());
				// 목적 로우를 부름
				CZone target_row=dataSet.getM_lstZone().get(tobay.getZone());				
				// 1. 출발 로우의 베이가 아래에 위치한 경우
				if(frombay.isM_isBayUpperdirection()) {					
					// 목적 로우의 베이가 아래에 위치한 경우
					if(tobay.isM_isBayUpperdirection()) {						
						CZone opposite_row;						
						if(tobay.getZone()<frombay.getZone()) {
							// 목적 로우가 위쪽에 위치하면 목적 로우의 맞은편 로우 호출
							opposite_row=dataSet.getM_lstZone().get(tobay.getZone()-1);	
						}
							// 출발 로우가 위쪽에 위치하면 출발 로우의 맞은편 로우 호출
						else {
							opposite_row=dataSet.getM_lstZone().get(frombay.getZone()-1);
						}						
						// 로우 좌측까지 이동
						zonetozone+=starting_x_central_loop-Math.min(now_row.getLeft(), opposite_row.getLeft());
						// 목적 로우의 맞은편 로우로 이동
						zonetozone+=opposite_row.getTop()-(now_row.getTop()+now_row.getHeight());
						// 맞은편 로우의 오른쪽으로 이동
						zonetozone+=Math.max(now_row.getLeft()+now_row.getWidth(),opposite_row.getLeft()+opposite_row.getWidth())-Math.min(now_row.getLeft(), opposite_row.getLeft());
						// 목적 로우로 이동(남쪽 방향으로 이동) 
						zonetozone+=opposite_row.getTop()-(target_row.getTop()+target_row.getHeight());
						// 목적 로우 내 진입 베이까지 이동
						zonetozone+=(target_row.getLeft()+target_row.getWidth())-ending_x_central_loop;							
					}
					// 목적 로우의 베이가 위에 위치한 경우
					else {
						// 목적 로우가 출발 로우와 마주보고 있는 경우
						if(tobay.getZone()==frombay.getZone()-1) {
							// 로우 좌측까지 이동
							zonetozone+=starting_x_central_loop-Math.min(now_row.getLeft(), target_row.getLeft());
							// 목적 로우로 이동
							zonetozone+=target_row.getTop()-(now_row.getTop()+now_row.getHeight());
							// 목적 로우 내 집입 베이까지 이동
							zonetozone+=ending_x_central_loop-Math.min(now_row.getLeft(), target_row.getLeft());							
						}
						// 목적 로우가 출발 로우와 마주보고 있지 않는 경우
						else {
							// 목적 로우가 아래에 위치한 경우							
							if(tobay.getZone()>frombay.getZone()) {
								CZone opposite_fromrow=dataSet.getM_lstZone().get(frombay.getZone()-1);
								CZone opposite_torow=dataSet.getM_lstZone().get(tobay.getZone()+1);
								// 로우 좌측까지 이동
								zonetozone+=starting_x_central_loop-Math.min(now_row.getLeft(), opposite_fromrow.getLeft());
								// 출발 로우의 맞은편으로 이동
								zonetozone+=opposite_fromrow.getTop()-(now_row.getTop()+now_row.getHeight());								
								// 출발 로우 맞은편의 오른쪼으로 이동
								zonetozone+=Math.max(opposite_torow.getLeft()+opposite_torow.getWidth(), opposite_fromrow.getLeft()+opposite_fromrow.getWidth())-Math.min(now_row.getLeft(), opposite_fromrow.getLeft());
								// 출발 로우의 맞으편에서 목적 로우의 맞은 편으로 이동
								zonetozone+=opposite_fromrow.getTop()-(opposite_torow.getTop()+opposite_torow.getHeight());
								// 목적 로우의 맞은편 왼쪽으로 이동
								zonetozone+=Math.max(opposite_torow.getLeft()+opposite_torow.getWidth(), target_row.getLeft()+target_row.getWidth())-Math.min(opposite_torow.getLeft(), target_row.getLeft());
								// 목적 로우의 맞은편에서 목적 로우로 이동
								zonetozone+=target_row.getTop()-(opposite_torow.getTop()+opposite_torow.getHeight());
								// 목적 로우에서 진입 베이로 이동
								zonetozone+=ending_x_central_loop-Math.min(opposite_torow.getLeft(), target_row.getLeft());
							}
							// 목적 로우가 위에 위치한 경우
							else {
								// 로우 좌측까지 이동
								zonetozone+=starting_x_central_loop-Math.min(now_row.getLeft(), target_row.getLeft());
								zonetozone+=target_row.getTop()-(now_row.getTop()+now_row.getHeight());
								zonetozone+=ending_x_central_loop-Math.min(now_row.getLeft(), target_row.getLeft());								
							}
						}
					}
				}				
				// 2. 출발 로우의 베이가 위에 위치한 경우
				else {
					// 목적 로우의 베이가 아래에 위치한 경우
					if(tobay.isM_isBayUpperdirection()) {
						// 목적 로우가 아래에 위치한 경우							
						if(tobay.getZone()>frombay.getZone()) {
							// 출발 로우 우측까지 이동
							zonetozone+=starting_x_central_loop-Math.min(now_row.getLeft(), target_row.getLeft());
							// 목적 로우로 이동 
							zonetozone+=now_row.getTop()-(target_row.getTop()+target_row.getHeight());
							zonetozone+=Math.max(now_row.getLeft()+now_row.getWidth(), target_row.getLeft()+target_row.getWidth())-ending_x_central_loop;
						}
						// 목적 로우가 위에 위치한 경우
						else {							
							CZone opposite_fromrow=dataSet.getM_lstZone().get(frombay.getZone()+1);
							CZone opposite_torow=dataSet.getM_lstZone().get(tobay.getZone()-1);
							// 로우 좌측까지 이동
							zonetozone+=Math.max(now_row.getLeft()+now_row.getWidth(), opposite_fromrow.getLeft()+opposite_fromrow.getWidth())-starting_x_central_loop;
							// 출발 로우의 맞은편으로 이동
							zonetozone+=(opposite_fromrow.getTop()+opposite_fromrow.getHeight())-now_row.getTop();								
							// 출발 로우 맞은편 로우의 왼쪽으로 이동
							zonetozone+=Math.max(now_row.getLeft()+now_row.getWidth(), opposite_fromrow.getLeft()+opposite_fromrow.getWidth())-Math.min(opposite_torow.getLeft(), opposite_fromrow.getLeft());							
							// 출발 로우의 맞으편에서 목적 로우의 맞은 편으로 이동
							zonetozone+=opposite_torow.getTop()-(opposite_fromrow.getTop()+opposite_fromrow.getHeight());
							// 목적 로우의 맞은편 왼쪽으로 이동
							zonetozone+=Math.max(opposite_torow.getLeft()+opposite_torow.getWidth(), target_row.getLeft()+target_row.getWidth())-Math.min(opposite_torow.getLeft(), target_row.getLeft());
							// 목적 로우의 맞은편에서 목적 로우로 이동
							zonetozone+=opposite_torow.getTop()-(target_row.getTop()+target_row.getHeight());
							// 목적 로우에서 진입 베이로 이동
							zonetozone+=Math.max(opposite_torow.getLeft()+opposite_torow.getWidth(), target_row.getLeft()+target_row.getWidth())-ending_x_central_loop;
						}
					}
					// 목적 로우의 베이가 위에 위치한 경우
					else {
						CZone opposite_row;						
						if(tobay.getZone()>frombay.getZone()) {
							// 목적 로우가 위쪽에 위치하면 출발 로우의 맞은편 로우 호출
							opposite_row=dataSet.getM_lstZone().get(frombay.getZone()+1);	
						}
							// 출발 로우가 위쪽에 위치하면 목적 로우의 맞은편 로우 호출
						else {
							opposite_row=dataSet.getM_lstZone().get(tobay.getZone()+1);
						}						
						// 로우 우측까지 이동
						zonetozone+=Math.max(now_row.getLeft()+now_row.getWidth(), opposite_row.getLeft()+opposite_row.getWidth())-starting_x_central_loop;
						// 맞은편 로우로 이동
						zonetozone+=now_row.getTop()-(opposite_row.getTop()+opposite_row.getHeight());
						// 맞은편 로우의 좌측으로 이동
						zonetozone+=Math.max(now_row.getLeft()+now_row.getWidth(),opposite_row.getLeft()+opposite_row.getWidth())-Math.min(target_row.getLeft(), opposite_row.getLeft());
						// 목적 로우로 이동 
						zonetozone+=target_row.getTop()-(opposite_row.getTop()+opposite_row.getHeight());
						// 목적 로우 내 진입 베이까지 이동
						zonetozone+=ending_x_central_loop-Math.min(target_row.getLeft(), opposite_row.getLeft());		
					}					
				}
			}				
		}	
		return eqtozone+zonetozone+zonetoeq;
	}
	/**
	 * 
	 * @author : jwon.cho
	 * @version : 2010. 12. 08 오후 5:22:27
	 * @date : 2010. 12. 08
	 * @param population
	 * @return
	 * @변경이력 :
	 * @Method 설명 : EQ와 다른 Room 간의 반송량 계산
	 */
	private MicroOrganism calFitnessByDistance(MicroOrganism population, CRoom fromRoom)
	{
		EQ fromEQ;
		CRoom toRoom;
		String fromEQName, toEQName, toRoomName;
		String fromRoomName = fromRoom.getM_strName();
		String key;
		double freq;
		double distance;
		double totalDistance = 0;
		
		// ksn 이동 거리 기준 fitness를 다시 계산해야함.
		// from EQ to Room
		ArrayList<EQ> eqListOfPopulation = population.getChromosomeByName();
		ArrayList<CRoom> roomList = resultSet.getM_bestRoomOrder().getChromosomeByName();
		Hashtable<String, Double> distanceToEachRoom = new Hashtable<String, Double>(); 
		
		for (int i = 0; i < eqListOfPopulation.size(); i++) {
			fromEQ = eqListOfPopulation.get(i);
			fromEQName = fromEQ.getDeviceName();
			
			for (int j = 0; j < roomList.size(); j++) {
				toRoom = roomList.get(j);
				toRoomName = toRoom.getM_strName();
				if(dataSet.CRITERION_IS_DISTANCE_2){ //H2 전용. room은 하나뿐임. 레일 고려 없이 그냥 가까우면 됨.
					if(toRoomName.equals(fromRoomName)){
						ArrayList<EQ> eqList = toRoom.getM_lstEQ();
						for (int k = 0; k < eqList.size(); k++) {
							toEQName = eqList.get(k).getDeviceName();
							freq = 0;
							key = fromEQName + "$" + toEQName;
							if(null == dataSet.getM_htFromTo_EQ().get(key)) {}
							else freq = Float.parseFloat(dataSet.getM_htFromTo_EQ().get(key).toString());
							distance = calculateDistanceInSameRoom2(fromEQ, eqList.get(k), fromRoom, freq);
							addDistance(distanceToEachRoom, toRoomName, ((distance / 1000000)/ 2));
							totalDistance += distance;
						}
					}
				} else {
					if(toRoomName.equals(fromRoomName)){
						// 동일 공정 내 반송 - 나가는 반송만 계산해서 2를 곱해줌
						// resultSet의 eqList는 기존에 배치되어 저장된 것이므로 그 리스트를 사용하면 안됨.
						for (int k = 0; k < eqListOfPopulation.size(); k++) {
							freq = 0;
							toEQName = eqListOfPopulation.get(k).getDeviceName();
							key = fromEQName + "$" + toEQName;
							if(null == dataSet.getM_htFromTo_EQ().get(key)) {}
							else freq = Float.parseFloat(dataSet.getM_htFromTo_EQ().get(key).toString());
							distance = calculateDistanceFromEqToEq(fromEQ, eqListOfPopulation.get(k), freq);
							addDistance(distanceToEachRoom, toRoomName, ((distance / 1000000)/ 2));
							totalDistance += distance;
							
							freq = 0;
							key = toEQName + "$" + fromEQName;
							if(null == dataSet.getM_htFromTo_EQ().get(key)) {}
							else freq = Float.parseFloat(dataSet.getM_htFromTo_EQ().get(key).toString());
							distance = calculateDistanceFromEqToEq(eqListOfPopulation.get(k), fromEQ, freq);
							addDistance(distanceToEachRoom, toRoomName, ((distance / 1000000)/ 2));
							totalDistance += distance;
						}
					} else {
						if(!toRoom.isEqArranged()){
							// 다른 Room으로 보내는 반송
							freq = 0;
							distance = 0;
							key = fromEQName + "$" + toRoomName;
							
							if(null == dataSet.getM_htFromEQ_ToRoom().get(key)) {}
							else freq = Float.parseFloat(dataSet.getM_htFromEQ_ToRoom().get(key).toString());
							
							distance += calculateDistanceFromEqToRoom(fromEQ, fromRoom, toRoom, freq);
							
							// 다른 Room에서 들어오는 반송
							freq = 0;
							key = toRoomName + "$" + fromEQName;
							
							if(null == dataSet.getM_htFromRoom_ToEQ().get(key)) {}
							else freq = Float.parseFloat(dataSet.getM_htFromRoom_ToEQ().get(key).toString());
							
							distance += calculateDistanceFromRoomToEq(toRoom, fromRoom, fromEQ, freq);
							addDistance(distanceToEachRoom, toRoomName, ((distance / 1000000)/ 2));
							totalDistance += distance;
						} else {
							ArrayList<EQ> eqList = toRoom.getM_lstEQ();
							for (int k = 0; k < eqList.size(); k++) {
								// 다른 Room으로 보내는 반송
								freq = 0;
								distance = 0.0d;
								toEQName = eqList.get(k).getDeviceName();
								key = fromEQName + "$" + toEQName;
								if(null == dataSet.getM_htFromTo_EQ().get(key)) {}
								else freq = Float.parseFloat(dataSet.getM_htFromTo_EQ().get(key).toString());	
								distance += calculateDistanceFromEqToEq(fromEQ, eqList.get(k), freq);
								
								// 다른 Room에서 들어오는 반송
								freq = 0;
								key = toEQName + "$" + fromEQName;
								if(null == dataSet.getM_htFromTo_EQ().get(key)) {}
								else freq = Float.parseFloat(dataSet.getM_htFromTo_EQ().get(key).toString());	
								distance += calculateDistanceFromEqToEq(eqList.get(k), fromEQ, freq);
								
								addDistance(distanceToEachRoom, toRoomName, ((distance / 1000000)/ 2));
								
								totalDistance += distance;
							}
						}
					}
				}
			}
		}
		population.setDistanceToEachRoom(distanceToEachRoom);
		totalDistance = totalDistance / 1000000; //mm 단위를 km으로
		totalDistance = totalDistance / 2; // 나가는 반송과 들어오는 반송을 중복으로 계산하였으므로, 2로 나눠줌.
		population.setFitnessByDistance(totalDistance);
		
		double recalculatedFitness;
		for (int i = 0; i < roomList.size(); i++) {
			toRoom = roomList.get(i);
			toRoomName = toRoom.getM_strName();
			// fromRoom에서 나가는 건 위에서 계산했고,
			if(!fromRoomName.equals(toRoomName)){
				if(toRoom.isEqArranged()){
					// 기존 공정에 대한 fitness function 재계산
					recalculatedFitness = reCalFitnessByDistance(toRoom, population, fromRoom); 
					totalDistance += recalculatedFitness;
				}
			}
		}
		population.setIncrementalFitnessByDistance(totalDistance);
		return population;
	}

	private void addDistance(Hashtable<String, Double> distanceToEachRoom, String toRoomName, double distance) {
		double tmp = 0;
		if(distanceToEachRoom.containsKey(toRoomName)){
			tmp = distanceToEachRoom.get(toRoomName);
			distanceToEachRoom.remove(toRoomName);
		}
		distanceToEachRoom.put(toRoomName, (tmp + distance));
	}

	/**
	 * 기 배치된 공정에서 계산된 거리를 가져오되, 이번에 배치된 공정과의 거리는 이번에 계산한 값으로 치환하여 가져욤
	 * @param fromRoom
	 * @param thisPopulation
	 * @param thisRoom
	 * @return
	 */
	private double reCalFitnessByDistance(CRoom fromRoom, MicroOrganism thisPopulation, CRoom thisRoom)
	{
		CRoom toRoom;
		String fromEqName, toEqName, toRoomName;
		String fromRoomName = fromRoom.getM_strName();
		double distance;
		double totalDistance = 0;
		ArrayList<CRoom> roomList = resultSet.getM_bestRoomOrder().getChromosomeByName();
		// 이번에 막 배치된 공정으로의 반송 거리 보정
		for (int i = 0; i < roomList.size(); i++) {
			toRoom = roomList.get(i);
			toRoomName = toRoom.getM_strName();
			if(toRoomName.equals(thisRoom.getM_strName())){ // 이번에 배치된 공정과의 거리 값만 업데이트.
				distance = thisPopulation.getDistanceToEachRoom().get(fromRoomName);
				totalDistance += distance;
			} else {
				totalDistance += fromRoom.getDistanceToEachRoom().get(toRoomName);
			}
		}
		return totalDistance;
	}
	
	/**
	 * @author jwon.cho
	 * @Method 설명: EQ-Room 거리 구함
	 * eq에서 레일 방향성에 따라 나갈 수 있는 갓길까지 나가고, 나간 직후부터 도착 room 중심까지의 거리를 더한다.
	 * room이 두 개인 경우(세 개 이상은 고려하지 않음) 룸의 넓이 비율로 양쪽 룸으로 간다고 보고 계산.
	 * 동일 zone 여부에 따라 가중치를 다르게 적용.
	 */
	private double calculateDistanceFromEqToRoom(EQ fromEQ, CRoom fromRoom, CRoom toRoom, double freq) {
		double calculatedDistance = 0.0f;
		//subRoom이 두 개 있는 경우에 대한 로직 추가.. 3개 이상인 경우는 없을 것으로 가정 jwon.cho
		double fromEqPortX, fromEqPortY; // 반송이 나가는 설비의 포트 좌표
		double sideX, sideY; // 반송이 나가는 설비에서 옆으로 나갔을 때의 좌표
		CSubRoom toSubRoom_1, toSubRoom_2;
		double toRoomCenterX_1, toRoomCenterY_1, toRoomCenterX_2, toRoomCenterY_2; // 반송이 들어가는 룸의 중심 좌표
		// 포트에서 갓길로, 갓길에서 룸 중심까지 거리, 합계(가중치 부여)
		double distancePortToSide, distanceSideToRoomCenter_1, distanceSideToRoomCenter_2, distanceSum_1, distanceSum_2; 
boolean printSeletedEq = false;
//boolean printSeletedEq = fromEQ.isSelected();
if(printSeletedEq) System.out.println("****************fromEQ.getDeviceName(): " + fromEQ.getDeviceName() + " / toRoom: " + toRoom.getM_strName());
		//fromEQ Zone의 배치 방향에 따라 다르게 계산해야 함.
		CZone zone = getZone(fromEQ);
		if(zone.getType().equals("가로")){
			// 1. Port 위치 계산
			if(fromEQ.isM_isEQUpperDirection()) fromEqPortX = fromEQ.getCoordLeft() + fromEQ.getCoordWidth();
			else fromEqPortX = fromEQ.getCoordLeft();
			fromEqPortY = fromEQ.getCoordTop() + fromEQ.getCoordLength() / 2;
if(printSeletedEq) System.out.println("fromEQ.getCoordLeft(): " + fromEQ.getCoordLeft());				
if(printSeletedEq) System.out.println("fromEQ.getCoordWidth(): " + fromEQ.getCoordWidth());
if(printSeletedEq) System.out.println("fromEQ.getCoordTop(): " + fromEQ.getCoordTop());				
if(printSeletedEq) System.out.println("fromEQ.getCoordLength(): " + fromEQ.getCoordLength());
if(printSeletedEq) System.out.println("fromEqPortX: " + fromEqPortX);			
if(printSeletedEq) System.out.println("fromEqPortY: " + fromEqPortY);
			// 2. 레일 방향을 보고 갓길 위치 계산
			if(dataSet.IS_RAIL_IN_BAY_CLOCKWISE){
//				if(fromEQ.isM_isEQUpperDirection()) sideY = fromRoom.getM_lstSubRoom().get(0).getTop() +fromRoom.getM_lstSubRoom().get(0).getHeight(); // 위로 나감
//				else sideY = fromRoom.getM_lstSubRoom().get(0).getTop(); //아래로 나감
				if(fromEQ.isM_isEQUpperDirection()) sideY = zone.getTop() + zone.getHeight(); // 위로 나감
				else sideY = zone.getTop(); //아래로 나감
			} else {
//				if(fromEQ.isM_isEQUpperDirection()) sideY = fromRoom.getM_lstSubRoom().get(0).getTop(); //아래로 나감
//				else sideY = fromRoom.getM_lstSubRoom().get(0).getTop() + fromRoom.getM_lstSubRoom().get(0).getHeight(); // 위로 나감
				if(fromEQ.isM_isEQUpperDirection()) sideY = zone.getTop(); //아래로 나감
				else sideY = zone.getTop() + zone.getHeight(); // 위로 나감
			}
			sideX = fromEqPortX;
if(printSeletedEq) System.out.println("sideX: " + sideX);			
if(printSeletedEq) System.out.println("sideY: " + sideY);
			// 3. eq - side 거리 계산
			distancePortToSide = Math.abs(sideX - fromEqPortX) + Math.abs(sideY - fromEqPortY);
if(printSeletedEq) System.out.println("distancePortToSide: " + distancePortToSide);			
			
			///// toRoom이 하나일 때
			if(toRoom.getM_lstSubRoom().size() < 2){
				toSubRoom_1 = toRoom.getM_lstSubRoom().get(0);
				
				// 4-1. toRoom 중심 위치 계산
				toRoomCenterX_1 = toSubRoom_1.getLeft() + toSubRoom_1.getWidth() / 2;
				toRoomCenterY_1 = toSubRoom_1.getTop() + toSubRoom_1.getHeight() / 2;
if(printSeletedEq) System.out.println("toRoomCenterX_1: " + toRoomCenterX_1);				
if(printSeletedEq) System.out.println("toRoomCenterY_1: " + toRoomCenterY_1);
				// 5-1. side - room 거리 계산
				distanceSideToRoomCenter_1 = Math.abs(sideX - toRoomCenterX_1) + Math.abs(sideY - toRoomCenterY_1);
if(printSeletedEq) System.out.println("distanceSideToRoomCenter_1: " + distanceSideToRoomCenter_1);				
				// 6-1. 전체 거리 합치고 가중치 곱하기
				if(fromEQ.getZoneIndex() == toSubRoom_1.getM_nZoneIndex()) //// fromEq와 toRoom의 zone이 같을 때
					distanceSum_1 = (distancePortToSide + distanceSideToRoomCenter_1) * CDataSet.MICRO_ROOM_PENALTY;
				else //// fromEq와 toRoom의 zone이 다를 때
					distanceSum_1 = (distancePortToSide + distanceSideToRoomCenter_1) * CDataSet.MICRO_ZONE_PENALTY;
if(printSeletedEq) System.out.println("distanceSum_1: " + distanceSum_1);				
				distanceSum_2 = 0;
if(printSeletedEq) System.out.println("distanceSum_2: " + distanceSum_2);				
				///// toRoom이 두 개일 때(세 개 이상은 고려하지 않음)	
			} else {
				toSubRoom_1 = toRoom.getM_lstSubRoom().get(0);
				toSubRoom_2 = toRoom.getM_lstSubRoom().get(1);
				
				// 4-2. toRoom 중심 위치 계산
				toRoomCenterX_1 = toSubRoom_1.getLeft() + toSubRoom_1.getWidth()/2;
				toRoomCenterY_1 = toSubRoom_1.getTop() + toSubRoom_1.getHeight()/2;
if(printSeletedEq) System.out.println("toRoomCenterX_1: " + toRoomCenterX_1);				
if(printSeletedEq) System.out.println("toRoomCenterY_1: " + toRoomCenterY_1);				
				toRoomCenterX_2 = toSubRoom_2.getLeft() + toSubRoom_2.getWidth()/2;
				toRoomCenterY_2 = toSubRoom_2.getTop() + toSubRoom_2.getHeight()/2;
if(printSeletedEq) System.out.println("toRoomCenterX_2: " + toRoomCenterX_2);				
if(printSeletedEq) System.out.println("toRoomCenterY_2: " + toRoomCenterY_2);
				// 5-2. side - room 거리 계산 
				distanceSideToRoomCenter_1 = Math.abs(sideX - toRoomCenterX_1) + Math.abs(sideY - toRoomCenterY_1);
				distanceSideToRoomCenter_2 = Math.abs(sideX - toRoomCenterX_2) + Math.abs(sideY - toRoomCenterY_2);
if(printSeletedEq) System.out.println("distanceSideToRoomCenter_1: " + distanceSideToRoomCenter_1);				
if(printSeletedEq) System.out.println("distanceSideToRoomCenter_2: " + distanceSideToRoomCenter_2);
				double area_1 = toSubRoom_1.getWidth() * toSubRoom_1.getHeight();
				double area_2 = toSubRoom_2.getWidth() * toSubRoom_2.getHeight();
if(printSeletedEq) System.out.println("area_1: " + area_1);
if(printSeletedEq) System.out.println("area_2: " + area_2);
				// 6-2. 전체 거리 합치고 가중치 곱하기
				if(area_1 + area_2 > 0){
					if(fromEQ.getZoneIndex() == toSubRoom_1.getM_nZoneIndex()) //// fromEq와 toRoom의 zone이 같을 때 
						distanceSum_1 = (distancePortToSide + distanceSideToRoomCenter_1) * CDataSet.MICRO_ROOM_PENALTY * (area_1 / (area_1 + area_2));
					else //// fromEq와 toRoom의 zone이 다를 때
						distanceSum_1 = (distancePortToSide + distanceSideToRoomCenter_1) * CDataSet.MICRO_ZONE_PENALTY * (area_1 / (area_1 + area_2));
if(printSeletedEq) System.out.println("distanceSum_1: " + distanceSum_1);					
					if(fromEQ.getZoneIndex() == toSubRoom_2.getM_nZoneIndex()) //// fromEq와 toRoom의 zone이 같을 때
						distanceSum_2 = (distancePortToSide + distanceSideToRoomCenter_2) * CDataSet.MICRO_ROOM_PENALTY * (area_2 / (area_1 + area_2));
					else //// fromEq와 toRoom의 zone이 다를 때
						distanceSum_2 = (distancePortToSide + distanceSideToRoomCenter_2) * CDataSet.MICRO_ZONE_PENALTY * (area_2 / (area_1 + area_2));
if(printSeletedEq) System.out.println("distanceSum_2: " + distanceSum_2);
				} else {
					// 두 subRoom의 크기 합이 0이면 계산 불가.
					distanceSum_1 = 0;
					distanceSum_2 = 0;
				}
			}
		} else {
			// 1. Port 위치 계산
			fromEqPortX = fromEQ.getCoordLeft() + fromEQ.getCoordWidth() / 2;
			if(fromEQ.isM_isEQUpperDirection()) fromEqPortY = fromEQ.getCoordTop() + fromEQ.getCoordLength();
			else fromEqPortY = fromEQ.getCoordTop();
			
			if(printSeletedEq) System.out.println("fromEQ.getCoordLeft(): " + fromEQ.getCoordLeft());				
			if(printSeletedEq) System.out.println("fromEQ.getCoordWidth(): " + fromEQ.getCoordWidth());
			if(printSeletedEq) System.out.println("fromEQ.getCoordTop(): " + fromEQ.getCoordTop());				
			if(printSeletedEq) System.out.println("fromEQ.getCoordLength(): " + fromEQ.getCoordLength());
			if(printSeletedEq) System.out.println("fromEqPortX: " + fromEqPortX);			
			if(printSeletedEq) System.out.println("fromEqPortY: " + fromEqPortY);
			
			// 2. 레일 방향을 보고 갓길 위치 계산
			if(dataSet.IS_RAIL_IN_BAY_CLOCKWISE){
				if(fromEQ.isM_isEQUpperDirection()) sideX = zone.getLeft(); //왼쪽으로 나감
				else sideX = zone.getLeft() + zone.getWidth(); // 오른쪽으로 나감
			} else {
				if(fromEQ.isM_isEQUpperDirection()) sideX = zone.getLeft() + zone.getWidth(); // 오른쪽으로 나감
				else sideX = zone.getLeft(); //왼쪽으로 나감
			}
			sideY = fromEqPortY;
			if(printSeletedEq) System.out.println("sideX: " + sideX);			
			if(printSeletedEq) System.out.println("sideY: " + sideY);
			// 3. eq - side 거리 계산
			distancePortToSide = Math.abs(sideX-fromEqPortX) + Math.abs(sideY-fromEqPortY);
			if(printSeletedEq) System.out.println("distancePortToSide: " + distancePortToSide);
			
			///// toRoom이 하나일 때
			if(toRoom.getM_lstSubRoom().size() < 2){
				toSubRoom_1 = toRoom.getM_lstSubRoom().get(0);
				
				// 4-1. toRoom 중심 위치 계산
				toRoomCenterX_1 = toSubRoom_1.getLeft() + toSubRoom_1.getWidth() / 2;
				toRoomCenterY_1 = toSubRoom_1.getTop() + toSubRoom_1.getHeight() / 2;
				if(printSeletedEq) System.out.println("toRoomCenterX_1: " + toRoomCenterX_1);				
				if(printSeletedEq) System.out.println("toRoomCenterY_1: " + toRoomCenterY_1);
				// 5-1. side - room 거리 계산
				distanceSideToRoomCenter_1 = Math.abs(sideX - toRoomCenterX_1) + Math.abs(sideY - toRoomCenterY_1);
				if(printSeletedEq) System.out.println("distanceSideToRoomCenter_1: " + distanceSideToRoomCenter_1);
				// 6-1. 전체 거리 합치고 가중치 곱하기
				if(fromEQ.getZoneIndex() == toSubRoom_1.getM_nZoneIndex()) //// fromEq와 toRoom의 zone이 같을 때
					distanceSum_1 = (distancePortToSide + distanceSideToRoomCenter_1) * CDataSet.MICRO_ROOM_PENALTY;
				else //// fromEq와 toRoom의 zone이 다를 때
					distanceSum_1 = (distancePortToSide + distanceSideToRoomCenter_1) * CDataSet.MICRO_ZONE_PENALTY; 
				if(printSeletedEq) System.out.println("distanceSum_1: " + distanceSum_1);
				distanceSum_2 = 0;
				///// toRoom이 두 개일 때(세 개 이상은 고려하지 않음)	
			} else {
				toSubRoom_1 = toRoom.getM_lstSubRoom().get(0);
				toSubRoom_2 = toRoom.getM_lstSubRoom().get(1);
				
				// 4-2. toRoom 중심 위치 계산
				toRoomCenterX_1 = toSubRoom_1.getLeft() + toSubRoom_1.getWidth()/2;
				toRoomCenterY_1 = toSubRoom_1.getTop() + toSubRoom_1.getHeight()/2;
				
				toRoomCenterX_2 = toSubRoom_2.getLeft() + toSubRoom_2.getWidth()/2;
				toRoomCenterY_2 = toSubRoom_2.getTop() + toSubRoom_2.getHeight()/2;
				
				// 5-2. side - room 거리 계산 
				distanceSideToRoomCenter_1 = Math.abs(sideX - toRoomCenterX_1) + Math.abs(sideY - toRoomCenterY_1);
				distanceSideToRoomCenter_2 = Math.abs(sideX - toRoomCenterX_2) + Math.abs(sideY - toRoomCenterY_2);
				
				double area_1 = toSubRoom_1.getWidth() * toSubRoom_1.getHeight();
				double area_2 = toSubRoom_2.getWidth() * toSubRoom_2.getHeight();
				// 6-2. 전체 거리 합치고 가중치 곱하기
				if(area_1 + area_2 > 0){
					if(fromEQ.getZoneIndex() == toSubRoom_1.getM_nZoneIndex()) //// fromEq와 toRoom의 zone이 같을 때 
						distanceSum_1 = (distancePortToSide + distanceSideToRoomCenter_1) * CDataSet.MICRO_ROOM_PENALTY * (area_1 / (area_1 + area_2));
					else //// fromEq와 toRoom의 zone이 다를 때
						distanceSum_1 = (distancePortToSide + distanceSideToRoomCenter_1) * CDataSet.MICRO_ZONE_PENALTY * (area_1 / (area_1 + area_2));
					
					if(fromEQ.getZoneIndex() == toSubRoom_2.getM_nZoneIndex()) //// fromEq와 toRoom의 zone이 같을 때
						distanceSum_2 = (distancePortToSide + distanceSideToRoomCenter_2) * CDataSet.MICRO_ROOM_PENALTY * (area_2 / (area_1 + area_2));
					else //// fromEq와 toRoom의 zone이 다를 때
						distanceSum_2 = (distancePortToSide + distanceSideToRoomCenter_2) * CDataSet.MICRO_ZONE_PENALTY * (area_2 / (area_1 + area_2));
				} else {
					// 두 subRoom의 크기 합이 0이면 계산 불가.
					distanceSum_1 = 0;
					distanceSum_2 = 0;
				}
			}
		}
		
		//7. 반송량 곱하기
		calculatedDistance = freq * (distanceSum_1 + distanceSum_2);
if(printSeletedEq) System.out.println("freq: " + freq);		
if(printSeletedEq) System.out.println("freq * (distanceSum_1 + distanceSum_2): " + freq * (distanceSum_1 + distanceSum_2));
		return calculatedDistance;
	}
	
	/**
	 * @author jwon.cho
	 * @Method 설명: EQ-Room 거리 구함
	 * eq에서 레일 방향성에 따라 나갈 수 있는 갓길까지 나가고, 나간 직후부터 도착 room 중심까지의 거리를 더한다.
	 * room이 두 개인 경우(세 개 이상은 고려하지 않음) 룸의 넓이 비율로 양쪽 룸으로 간다고 보고 계산.
	 * 동일 zone 여부에 따라 가중치를 다르게 적용.
	 */
	private double calculateDistanceFromRoomToEq(CRoom fromRoom, CRoom toRoom, EQ toEQ, double freq) {
		double calculatedDistance = 0.0f;
		//subRoom이 두 개 있는 경우에 대한 로직 추가.. 3개 이상인 경우는 없을 것으로 가정 jwon.cho
		
		double toEqPortX, toEqPortY; // 반송이 들어가는 설비의 포트 좌표
		double sideX, sideY; // 반송이 들어가는 설비의 갓길 좌표
		CSubRoom fromSubRoom_1, fromSubRoom_2;
		double fromRoomCenterX_1, fromRoomCenterY_1, fromRoomCenterX_2, fromRoomCenterY_2; // 반송이 나가는 룸의 중심 좌표
		// 룸 중심에서 갓길로, 갓길에서 포트까지 거리, 합계(가중치 부여)
		double distanceRoomCenterToSide_1, distanceRoomCenterToSide_2, distanceSideToPort, distanceSum_1, distanceSum_2; 
boolean printSeletedEq = false;
//boolean printSeletedEq = toEQ.isSelected();
if(printSeletedEq) System.out.println("****************toEQ.getDeviceName(): " + toEQ.getDeviceName() + " / fromRoom: " + fromRoom.getM_strName());
		//fromEQ Zone의 배치 방향에 따라 다르게 계산해야 함.
		CZone zone = getZone(toEQ);
		if(zone.getType().equals("가로")){
			// 1. Port 위치 계산
			if(toEQ.isM_isEQUpperDirection()) toEqPortX = toEQ.getCoordLeft() + toEQ.getCoordWidth();
			else toEqPortX = toEQ.getCoordLeft();
			toEqPortY = toEQ.getCoordTop() + toEQ.getCoordLength() / 2;			
if(printSeletedEq) System.out.println("toEqPortX: " + toEqPortX);			
if(printSeletedEq) System.out.println("toEqPortY: " + toEqPortY);
			// 2. 레일 방향을 보고 갓길 위치 계산
			if(dataSet.IS_RAIL_IN_BAY_CLOCKWISE){
				if(toEQ.isM_isEQUpperDirection()) sideY = zone.getTop(); // 아래로 들어감
				else sideY = zone.getTop() + zone.getHeight(); //위로 들어감
			} else {
				if(toEQ.isM_isEQUpperDirection()) sideY = zone.getTop() + zone.getHeight(); // 위로 들어감
				else sideY = zone.getTop(); //아래로 들어감
			}
			sideX = toEqPortX;
if(printSeletedEq) System.out.println("sideX: " + sideX);			
if(printSeletedEq) System.out.println("sideY: " + sideY);
			// 3. eq - side 거리 계산
			distanceSideToPort = Math.abs(sideX - toEqPortX) + Math.abs(sideY - toEqPortY);
if(printSeletedEq) System.out.println("distancePortToSide: " + distanceSideToPort);			
			
			///// fromRoom이 하나일 때
			if(fromRoom.getM_lstSubRoom().size() < 2){
				fromSubRoom_1 = fromRoom.getM_lstSubRoom().get(0);
				
				// 4-1. toRoom 중심 위치 계산
				fromRoomCenterX_1 = fromSubRoom_1.getLeft() + fromSubRoom_1.getWidth() / 2;
				fromRoomCenterY_1 = fromSubRoom_1.getTop() + fromSubRoom_1.getHeight() / 2;
if(printSeletedEq) System.out.println("fromRoomCenterX_1: " + fromRoomCenterX_1);				
if(printSeletedEq) System.out.println("fromRoomCenterY_1: " + fromRoomCenterY_1);
				// 5-1. side - room 거리 계산
				distanceRoomCenterToSide_1 = Math.abs(sideX - fromRoomCenterX_1) + Math.abs(sideY - fromRoomCenterY_1);
if(printSeletedEq) System.out.println("distanceSideToRoomCenter_1: " + distanceRoomCenterToSide_1);				
				// 6-1. 전체 거리 합치고 가중치 곱하기
				if(toEQ.getZoneIndex() == fromSubRoom_1.getM_nZoneIndex()) //// toEq와 fromRoom의 zone이 같을 때
					distanceSum_1 = (distanceRoomCenterToSide_1 + distanceSideToPort) * CDataSet.MICRO_ROOM_PENALTY;
				else //// fromEq와 toRoom의 zone이 다를 때
					distanceSum_1 = (distanceRoomCenterToSide_1 + distanceSideToPort) * CDataSet.MICRO_ZONE_PENALTY;
if(printSeletedEq) System.out.println("distanceSum_1: " + distanceSum_1);				
				distanceSum_2 = 0;
if(printSeletedEq) System.out.println("distanceSum_2: " + distanceSum_2);				
				///// fromRoom이 두 개일 때(세 개 이상은 고려하지 않음)	
			} else {
				fromSubRoom_1 = fromRoom.getM_lstSubRoom().get(0);
				fromSubRoom_2 = fromRoom.getM_lstSubRoom().get(1);
				
				// 4-2. toRoom 중심 위치 계산
				fromRoomCenterX_1 = fromSubRoom_1.getLeft() + fromSubRoom_1.getWidth()/2;
				fromRoomCenterY_1 = fromSubRoom_1.getTop() + fromSubRoom_1.getHeight()/2;
if(printSeletedEq) System.out.println("fromRoomCenterX_1: " + fromRoomCenterX_1);				
if(printSeletedEq) System.out.println("fromRoomCenterY_1: " + fromRoomCenterY_1);				
				fromRoomCenterX_2 = fromSubRoom_2.getLeft() + fromSubRoom_2.getWidth()/2;
				fromRoomCenterY_2 = fromSubRoom_2.getTop() + fromSubRoom_2.getHeight()/2;
if(printSeletedEq) System.out.println("fromRoomCenterX_2: " + fromRoomCenterX_2);				
if(printSeletedEq) System.out.println("fromRoomCenterY_2: " + fromRoomCenterY_2);
				// 5-2. side - room 거리 계산 
				distanceRoomCenterToSide_1 = Math.abs(sideX - fromRoomCenterX_1) + Math.abs(sideY - fromRoomCenterY_1);
				distanceRoomCenterToSide_2 = Math.abs(sideX - fromRoomCenterX_2) + Math.abs(sideY - fromRoomCenterY_2);
if(printSeletedEq) System.out.println("distanceRoomCenterToSide_1: " + distanceRoomCenterToSide_1);				
if(printSeletedEq) System.out.println("distanceRoomCenterToSide_2: " + distanceRoomCenterToSide_2);
				double area_1 = fromSubRoom_1.getWidth() * fromSubRoom_1.getHeight();
				double area_2 = fromSubRoom_2.getWidth() * fromSubRoom_2.getHeight();
if(printSeletedEq) System.out.println("area_1: " + area_1);
if(printSeletedEq) System.out.println("area_2: " + area_2);
				// 6-2. 전체 거리 합치고 가중치 곱하기
				if(area_1 + area_2 > 0){
					if(toEQ.getZoneIndex() == fromSubRoom_1.getM_nZoneIndex()) //// fromEq와 toRoom의 zone이 같을 때 
						distanceSum_1 = (distanceSideToPort + distanceRoomCenterToSide_1) * CDataSet.MICRO_ROOM_PENALTY * (area_1 / (area_1 + area_2));
					else //// fromEq와 toRoom의 zone이 다를 때
						distanceSum_1 = (distanceSideToPort + distanceRoomCenterToSide_1) * CDataSet.MICRO_ZONE_PENALTY * (area_1 / (area_1 + area_2));
if(printSeletedEq) System.out.println("distanceSum_1: " + distanceSum_1);					
					if(toEQ.getZoneIndex() == fromSubRoom_2.getM_nZoneIndex()) //// fromEq와 toRoom의 zone이 같을 때
						distanceSum_2 = (distanceSideToPort + distanceRoomCenterToSide_2) * CDataSet.MICRO_ROOM_PENALTY * (area_2 / (area_1 + area_2));
					else //// fromEq와 toRoom의 zone이 다를 때
						distanceSum_2 = (distanceSideToPort + distanceRoomCenterToSide_2) * CDataSet.MICRO_ZONE_PENALTY * (area_2 / (area_1 + area_2));
if(printSeletedEq) System.out.println("distanceSideToPort: " + distanceSum_2);
				} else {
					// 두 subRoom의 크기 합이 0이면 계산 불가.
					distanceSum_1 = 0;
					distanceSum_2 = 0;
				}
			}
		} else {
			// 1. Port 위치 계산
			toEqPortX = toEQ.getCoordLeft() + toEQ.getCoordWidth() / 2;
			if(toEQ.isM_isEQUpperDirection()) toEqPortY = toEQ.getCoordTop() + toEQ.getCoordLength();
			else toEqPortY = toEQ.getCoordTop();
			
			// 2. 레일 방향을 보고 갓길 위치 계산
			if(dataSet.IS_RAIL_IN_BAY_CLOCKWISE){
				if(toEQ.isM_isEQUpperDirection()) sideX = zone.getLeft() + zone.getWidth(); // 오른쪽으로 들어감
				else sideX = zone.getLeft(); //왼쪽으로 들어감
			} else {
				if(toEQ.isM_isEQUpperDirection()) sideX = zone.getLeft(); //왼쪽으로 들어감
				else sideX = zone.getLeft() + zone.getWidth(); // 오른쪽으로 들어감
			}
			sideY = toEqPortY;
			
			// 3. eq - side 거리 계산
			distanceSideToPort = Math.abs(sideX - toEqPortX) + Math.abs(sideY - toEqPortY);
			
			///// fromRoom이 하나일 때
			if(fromRoom.getM_lstSubRoom().size() < 2){
				fromSubRoom_1 = fromRoom.getM_lstSubRoom().get(0);
				
				// 4-1. fromRoom 중심 위치 계산
				fromRoomCenterX_1 = fromSubRoom_1.getLeft() + fromSubRoom_1.getWidth() / 2;
				fromRoomCenterY_1 = fromSubRoom_1.getTop() + fromSubRoom_1.getHeight() / 2;
				
				// 5-1. side - room 거리 계산
				distanceRoomCenterToSide_1 = Math.abs(sideX - fromRoomCenterX_1) + Math.abs(sideY - fromRoomCenterY_1);
				
				// 6-1. 전체 거리 합치고 가중치 곱하기
				if(toEQ.getZoneIndex() == fromSubRoom_1.getM_nZoneIndex()) //// fromEq와 toRoom의 zone이 같을 때
					distanceSum_1 = (distanceRoomCenterToSide_1 + distanceSideToPort) * CDataSet.MICRO_ROOM_PENALTY;
				else //// fromEq와 toRoom의 zone이 다를 때
					distanceSum_1 = (distanceRoomCenterToSide_1 + distanceSideToPort) * CDataSet.MICRO_ZONE_PENALTY; 
				distanceSum_2 = 0;
				///// fromRoom 이 두 개일 때(세 개 이상은 고려하지 않음)	
			} else {
				fromSubRoom_1 = fromRoom.getM_lstSubRoom().get(0);
				fromSubRoom_2 = fromRoom.getM_lstSubRoom().get(1);
				
				// 4-2. fromRoom 중심 위치 계산
				fromRoomCenterX_1 = fromSubRoom_1.getLeft() + fromSubRoom_1.getWidth()/2;
				fromRoomCenterY_1 = fromSubRoom_1.getTop() + fromSubRoom_1.getHeight()/2;
				
				fromRoomCenterX_2 = fromSubRoom_2.getLeft() + fromSubRoom_2.getWidth()/2;
				fromRoomCenterY_2 = fromSubRoom_2.getTop() + fromSubRoom_2.getHeight()/2;
				
				// 5-2. side - room 거리 계산 
				distanceRoomCenterToSide_1 = Math.abs(sideX - fromRoomCenterX_1) + Math.abs(sideY - fromRoomCenterY_1);
				distanceRoomCenterToSide_2 = Math.abs(sideX - fromRoomCenterX_2) + Math.abs(sideY - fromRoomCenterY_2);
				
				double area_1 = fromSubRoom_1.getWidth() * fromSubRoom_1.getHeight();
				double area_2 = fromSubRoom_2.getWidth() * fromSubRoom_2.getHeight();
				// 6-2. 전체 거리 합치고 가중치 곱하기
				if(area_1 + area_2 > 0){
					if(toEQ.getZoneIndex() == fromSubRoom_1.getM_nZoneIndex()) //// toEq와 fromRoom의 zone이 같을 때 
						distanceSum_1 = (distanceRoomCenterToSide_1 + distanceSideToPort) * CDataSet.MICRO_ROOM_PENALTY * (area_1 / (area_1 + area_2));
					else //// fromEq와 toRoom의 zone이 다를 때
						distanceSum_1 = (distanceRoomCenterToSide_2 + distanceSideToPort) * CDataSet.MICRO_ZONE_PENALTY * (area_1 / (area_1 + area_2));
					
					if(toEQ.getZoneIndex() == fromSubRoom_2.getM_nZoneIndex()) //// fromEq와 toRoom의 zone이 같을 때
						distanceSum_2 = (distanceRoomCenterToSide_2 + distanceSideToPort) * CDataSet.MICRO_ROOM_PENALTY * (area_2 / (area_1 + area_2));
					else //// fromEq와 toRoom의 zone이 다를 때
						distanceSum_2 = (distanceRoomCenterToSide_2 + distanceSideToPort) * CDataSet.MICRO_ZONE_PENALTY * (area_2 / (area_1 + area_2));
				} else {
					// 두 subRoom의 크기 합이 0이면 계산 불가.
					distanceSum_1 = 0;
					distanceSum_2 = 0;
				}
			}
		}
		//7. 반송량 곱하기
		calculatedDistance = freq * (distanceSum_1 + distanceSum_2);
if(printSeletedEq) System.out.println("freq: " + freq);		
if(printSeletedEq) System.out.println("freq * (distanceSum_1 + distanceSum_2): " + freq * (distanceSum_1 + distanceSum_2));
		return calculatedDistance;
	}
	/**
	 * @author jwon.cho
	 * @param fromEQ
	 * @param toRoom
	 * @param totalDistance
	 * @param freq
	 * @return
	 * @Method 설명: 동일 Room 내에서 EQ-EQ 거리 구함
	 * zone이 같은 경우 Room의 양쪽 끝 중 가까운 쪽으로 나가서 갓길 또는 중앙통로를 지나 다시 들어간다고 가정
	 * 방향성을 고려하도록 수정해야 합니다!!!!!!!!!!!!!
	 */
	public double calculateDistanceFromEqToEq(EQ fromEQ, EQ toEQ, double freq)
	{
		if(fromEQ.getDeviceName().equals(toEQ.getDeviceName())) 
			return 0.0d; // 자신에 대해서는 계산하지 않음.
		double calculatedDistance;
		
		double fromEqPortX, fromEqPortY; // 반송이 나가는 설비의 포트 좌표
		double fromSideX, fromSideY; // 반송이 나가는 설비의 갓길 좌표
		double toEqPortX, toEqPortY; // 반송이 들어가는 설비의 포트 좌표
		double toSideX, toSideY; // 반송이 나가는 설비의 갓길 좌표
		double distancePortToSide, distanceSideToSide, distanceSideToPort; // fromEq-fromSide, fromSide-toSide, toSide-toEq
		boolean isFromToDirect = false; //반송이 갓길을 지날 필요 없이 바로 베이 내에서 베이 내로 갈 수 있나. 
		
		CZone fromZone = getZone(fromEQ);
//boolean printSeletedEq = fromEQ.isSelected();
		boolean printSeletedEq = false;
		if(printSeletedEq)
			System.out.println("fromEQ: " + fromEQ.getDeviceName() + " / toEQ: " + toEQ.getDeviceName());
		
		if(fromZone.getType().equals("가로"))
		{
			// 1. fromEQPort 위치 계산
			if(fromEQ.isM_isEQUpperDirection()) fromEqPortX = fromEQ.getCoordLeft() + fromEQ.getCoordWidth();
			else fromEqPortX = fromEQ.getCoordLeft();
			
			fromEqPortY = fromEQ.getCoordTop() + fromEQ.getCoordLength() / 2;
			
			// 2. 레일 방향을 보고 갓길 위치 계산
			if(dataSet.IS_RAIL_IN_BAY_CLOCKWISE)
			{
				if(fromEQ.isM_isEQUpperDirection()) fromSideY = fromZone.getTop() + fromZone.getHeight(); // 위로 나감
				else fromSideY = fromZone.getTop(); //아래로 나감
			}
			else 
			{
				if(fromEQ.isM_isEQUpperDirection()) fromSideY = fromZone.getTop(); //아래로 나감
				else fromSideY = fromZone.getTop() + fromZone.getHeight(); // 위로 나감
			}
			fromSideX = fromEqPortX;
			
		} 
		else 
		{
			// 1. fromEQPort 위치 계산
			fromEqPortX = fromEQ.getCoordLeft() + fromEQ.getCoordWidth() / 2;
			
			if(fromEQ.isM_isEQUpperDirection()) fromEqPortY = fromEQ.getCoordTop() + fromEQ.getCoordLength();
			else fromEqPortY = fromEQ.getCoordTop();
			
			// 2. 레일 방향을 보고 갓길 위치 계산
			if(dataSet.IS_RAIL_IN_BAY_CLOCKWISE){
				if(fromEQ.isM_isEQUpperDirection()) fromSideX = fromZone.getLeft(); //왼쪽으로 나감
				else fromSideX = fromZone.getLeft() + fromZone.getWidth(); // 오른쪽으로 나감
			} else {
				if(fromEQ.isM_isEQUpperDirection()) fromSideX = fromZone.getLeft() + fromZone.getWidth(); // 오른쪽으로 나감
				else fromSideX = fromZone.getLeft(); //왼쪽으로 나감
			}
			fromSideY = fromEqPortY;
		}
		// 3. fromEq - fromSide 거리 계산
		distancePortToSide = Math.abs(fromSideX - fromEqPortX) + Math.abs(fromSideY - fromEqPortY);
		
		if(printSeletedEq) 
			System.out.println("fromSideX: " + fromSideX + " / fromSideY: " + fromSideY);		
		if(printSeletedEq) 
			System.out.println("fromEqPortX: " + fromEqPortX + " / fromEqPortY: " + fromEqPortY);
		
		CZone toZone = getZone(toEQ);
		
		if(toZone.getType().equals("가로"))
		{
			// 1. toEQPort 위치 계산	
			if(toEQ.isM_isEQUpperDirection()) toEqPortX = toEQ.getCoordLeft() + toEQ.getCoordWidth();
			else toEqPortX = toEQ.getCoordLeft();
			
			toEqPortY = toEQ.getCoordTop() + toEQ.getCoordLength() / 2;
			
			// 2. 레일 방향을 보고 갓길 위치 계산
			if(dataSet.IS_RAIL_IN_BAY_CLOCKWISE)
			{
				if(toEQ.isM_isEQUpperDirection()) toSideY = toZone.getTop(); //아래로 들어옴
				else toSideY = toZone.getTop() + toZone.getHeight(); // 위로 들어옴
				
			} 
			else 
			{
				if(toEQ.isM_isEQUpperDirection()) toSideY = toZone.getTop() + toZone.getHeight(); // 위로 들어옴
				else toSideY = toZone.getTop(); //아래로 들어옴
				
			}
			toSideX = toEqPortX;
			
			if(fromEqPortX == toEqPortX)
			{
				if((fromEqPortY < toEqPortY && toEqPortY < fromSideY) || (fromEqPortY > toEqPortY && toEqPortY > fromSideY))
				{
					if(printSeletedEq) 
						System.out.println("다이렉트 반송");
					isFromToDirect = true;
				}
			}
		}
		else 
		{
			// 1. toEQPort 위치 계산
			toEqPortX = toEQ.getCoordLeft() + toEQ.getCoordWidth() / 2;
			
			if(toEQ.isM_isEQUpperDirection()) toEqPortY = toEQ.getCoordTop() + toEQ.getCoordLength();
			else toEqPortY = toEQ.getCoordTop();
			
			// 2. 레일 방향을 보고 갓길 위치 계산
			if(dataSet.IS_RAIL_IN_BAY_CLOCKWISE)
			{
				if(toEQ.isM_isEQUpperDirection()) toSideX = toZone.getLeft() + toZone.getWidth(); // 오른쪽으로 들어옴
				else toSideX = toZone.getLeft(); //왼쪽으로 들어옴
				
			}
			else
			{
				if(toEQ.isM_isEQUpperDirection()) toSideX = toZone.getLeft(); //왼쪽으로 들어옴
				else toSideX = toZone.getLeft() + toZone.getWidth(); // 오른쪽으로 들어옴
			}
			toSideY = toEqPortY;
			
			if(fromEqPortY == toEqPortY)
			{
				if((fromEqPortX < toEqPortX && toEqPortX < fromSideX) || (fromEqPortX > toEqPortX && toEqPortX > fromSideX))
				{
					if(printSeletedEq) 
						System.out.println("다이렉트 반송");
					isFromToDirect = true;
				}
			}
		}
		// 3. toSide - toEq 거리 계산
		distanceSideToPort = Math.abs(toSideX - toEqPortX) + Math.abs(toSideY - toEqPortY);
		
		if(printSeletedEq) System.out.println("toSideX: " + toSideX + " / toSideY: " + toSideY);		
		if(printSeletedEq) System.out.println("toEqPortX: " + toEqPortX + " / toEqPortY: " + toEqPortY);

		// 4. fromSide - toSide 거리 계산
		distanceSideToSide = Math.abs(toSideX - fromSideX) + Math.abs(toSideY - fromSideY);
		
		if(isFromToDirect)
		{
			calculatedDistance = freq * (Math.abs(fromEqPortX - toEqPortX) + Math.abs(fromEqPortY-toEqPortY)) * CDataSet.MICRO_ROOM_PENALTY;
		} 
		else 
		{
			if(fromZone.getZoneIndex() == toZone.getZoneIndex())
			{			
				calculatedDistance = freq * (distancePortToSide + distanceSideToSide + distanceSideToPort) * CDataSet.MICRO_ROOM_PENALTY;
			} 
			else 
			{
				calculatedDistance = freq * (distancePortToSide + distanceSideToSide + distanceSideToPort) * CDataSet.MICRO_ZONE_PENALTY;
			}
		}
		if(printSeletedEq) System.out.println("distancePortToSide: " + distancePortToSide + " / distanceSideToSide: " + distanceSideToSide
				+ " / distanceSideToPort: " + distanceSideToPort);
		if(printSeletedEq) System.out.println("freq: " + freq);
			return calculatedDistance;
	}
	/**
	 * @author jwon.cho
	 * @param fromEQ
	 * @param toRoom
	 * @param totalDistance
	 * @param freq
	 * @return
	 * @Method 설명: 동일 Room 내에서 EQ-EQ 거리 구함
	 * zone이 같은 경우 Room의 양쪽 끝 중 가까운 쪽으로 나가서 갓길 또는 중앙통로를 지나 다시 들어간다고 가정
	 * 방향성을 고려하도록 수정해야 합니다!!!!!!!!!!!!!
	 */
//	private double calculateDistanceInSameRoom333(EQ fromEQ, EQ toEQ, CRoom room, double totalDistance, double freq) {		
//		double x1 = fromEQ.getCoordLeft() + fromEQ.getCoordWidth()/2;
//		double y1 = fromEQ.getCoordTop() + fromEQ.getCoordLength()/2;
//		int fromZoneIndex = fromEQ.getZoneIndex();
//		
//		double x2 = toEQ.getCoordLeft() + toEQ.getCoordWidth()/2;
//		double y2 = toEQ.getCoordTop() + toEQ.getCoordLength()/2;
//		int toZoneIndex = toEQ.getZoneIndex();
//
//		if(fromZoneIndex == toZoneIndex){
//			//zone이 같으므로 fromRoomLeft와 toRoomLeft는 같은 값이지만, 의미상 구분
//			double fromRoomLeft = room.getM_lstSubRoom().get(0).getLeft();
//			double fromRoomRight = room.getM_lstSubRoom().get(0).getLeft() + room.getM_lstSubRoom().get(0).getWidth();
//			
//			double toRoomLeft = room.getM_lstSubRoom().get(0).getLeft();
//			double toRoomRight = room.getM_lstSubRoom().get(0).getLeft() + room.getM_lstSubRoom().get(0).getWidth();
//			
//			double distanceX = Math.min(Math.abs(x1-fromRoomLeft) + Math.abs(toRoomLeft-x2), Math.abs(x1-fromRoomRight) + Math.abs(toRoomRight-x2));
//
//			// 나가는 반송량과 들어오는 반송량을 계산해야 하는데, 그냥 여기서 2를 곱해주어 대신함
//			totalDistance += freq * (distanceX + Math.abs(y1 - y2)) * CDataSet.MICRO_ROOM_PENALTY * 2;
//		} else {
//			totalDistance += freq * (Math.abs(x1 - x2) + Math.abs(y1 - y2)) * CDataSet.MICRO_ZONE_PENALTY * 2; // 
//		}
//		return totalDistance;
//	}
	/**
	 * @author jwon.cho
	 * @param fromEQ
	 * @param toRoom
	 * @param totalDistance
	 * @param freq
	 * @return
	 * @Method 설명: 동일 Room 내에서 EQ-EQ 거리 구함
	 * H3 전용 계산
	 * 설비 중심 사용하되 바깥으로 나갔다 오지 않고, 바로 직각거리 계산.
	 */
	private double calculateDistanceInSameRoom2(EQ fromEQ, EQ toEQ, CRoom room, double freq) {
		double calculatedDistance = 0.0f;
		double x1 = fromEQ.getCoordLeft() + fromEQ.getCoordWidth()/2;
		double y1 = fromEQ.getCoordTop() + fromEQ.getCoordLength()/2;
		int fromZoneIndex = fromEQ.getZoneIndex();
		
		double x2 = toEQ.getCoordLeft() + toEQ.getCoordWidth()/2;
		double y2 = toEQ.getCoordTop() + toEQ.getCoordLength()/2;
		int toZoneIndex = toEQ.getZoneIndex();
		double distance1;
		double distance2;
		if(fromZoneIndex == toZoneIndex){
			//zone이 같으므로 fromRoomLeft와 toRoomLeft는 같은 값이지만, 의미상 구분
//			double fromRoomLeft = room.getM_lstSubRoom().get(0).getLeft();
//			double fromRoomRight = room.getM_lstSubRoom().get(0).getLeft() + room.getM_lstSubRoom().get(0).getWidth();
//			
//			double toRoomLeft = room.getM_lstSubRoom().get(0).getLeft();
//			double toRoomRight = room.getM_lstSubRoom().get(0).getLeft() + room.getM_lstSubRoom().get(0).getWidth();
//			
//			double distanceX = Math.min(Math.abs(x1-fromRoomLeft) + Math.abs(toRoomLeft-x2), Math.abs(x1-fromRoomRight) + Math.abs(toRoomRight-x2));
//			
//			totalDistance  += freq * (distanceX+Math.abs(y1-y2) * CDataSet.MICRO_ROOM_PENALTY); 
			calculatedDistance = freq * (Math.abs(x1-x2)+Math.abs(y1-y2)) * CDataSet.MICRO_ROOM_PENALTY; //
			distance1 =(Math.abs(x1-x2)+Math.abs(y1-y2)) * CDataSet.MICRO_ROOM_PENALTY;
			distance2 = freq * (Math.abs(x1-x2)+Math.abs(y1-y2)) * CDataSet.MICRO_ROOM_PENALTY; //
		} else {
			calculatedDistance = freq * (Math.abs(x1-x2)+Math.abs(y1-y2)) * CDataSet.MICRO_ZONE_PENALTY; //
			distance1 =(Math.abs(x1-x2)+Math.abs(y1-y2)) * CDataSet.MICRO_ZONE_PENALTY;
			distance2 = freq * (Math.abs(x1-x2)+Math.abs(y1-y2)) * CDataSet.MICRO_ZONE_PENALTY; //
		}
//		System.out.println("fromEQ.getDeviceName():" + fromEQ.getDeviceName() + " / toEQ.getDeviceName():" + toEQ.getDeviceName() + " / distance1:" + distance1 + " / distance:" + distance2);
		return calculatedDistance;
	}
	/**
	 * 
	 * @author : kyveri.kim
	 * @version : 2010. 07. 16 오전 9:18:31
	 * @date : 2010. 07. 16
	 * @param population
	 * @return
	 * @변경이력 :
	 * @Method 설명 : 변이 연산을 통해 해에 변화를 줌
	 */
	private MicroOrganism mutate(MicroOrganism population)
	{
		ArrayList<EQ> chromosomeByName = new ArrayList<EQ>();
		ArrayList<Integer> chromosomeByInt = new ArrayList<Integer>();

		// 난수를 발생하여 그 값이 파라미터(0.07)보다 작을 경우
		// size 내에 난수를 두 개 발생시켜 그 두 위치의 값을 교환
		int size = population.getChromosomeByInt().size();
		for (int i = 0; i < 2; i++)
		{
			if (Math.abs(Math.random()) < CDataSet.MICRO_MUTATE_RATIO)
			{
				int chooseLoc1 = new Random().nextInt(size);
				int chooseLoc2 = new Random().nextInt(size);
				int Num1 = population.getChromosomeByInt().get(chooseLoc1);
				int Num2 = population.getChromosomeByInt().get(chooseLoc2);

				int temp = Num1;
				population.getChromosomeByInt().set(chooseLoc1, Num2);
				population.getChromosomeByInt().set(chooseLoc2, temp);

				chromosomeByInt = population.getChromosomeByInt();
				chromosomeByName = population.getChromosomeByName();

				// 설비 배치 순서가 숫자 형태로 되어 있는 것을 이름으로 Mapping 하는 작업.
				chromosomeByName = eqMappingToName(chromosomeByInt, chromosomeByName);
				population.setChromosomeByName(chromosomeByName);

				// // 교환 후 값이 feasible 하지 않으면 수선

				// boolean isFeasibleChromosome = Chromo1.getFeasible(room);
				// if (!isFeasibleChromosome) repair(Chromo1);
			}
		}
		return population;
	}

	/**
	 * 
	 * @author : kyveri.kim
	 * @version : 2010. 07. 19 오전 9:28:39
	 * @date : 2010. 07. 19
	 * @변경이력 :
	 * @Method 설명 : 지정한 세대수 만큼 반복한 이후에 결과를 출력함.
	 */
	void finish()
	{
		elitism();
//		swap();
		if(CDataSet.DEBUG){
//			if(true){
			for (int i = 0; i < lstPopulations.size(); i++)
			{
	//			System.out.print("\n"+i + "th: " + lstPopulations.get(i).getFitnessByArea());
				System.out.print("\n"+i + "th: " + lstPopulations.get(i).getFitnessByDistance());
				for (int j = 0; j < lstPopulations.get(i).getChromosomeByInt().size(); j++)
				{
					System.out.print(" " + lstPopulations.get(i).getChromosomeByInt().get(j));
				}
			}
			System.out.println();
		}
		// resultSet에 결과 저장
		
		resultSet.getM_lstBayOrder().addAll(lstPopulations.get(0).getM_lstBay());
		// 각 eq에 저장된 zone 업데이트
		updateDataSetEq(lstPopulations.get(0).getM_lstBay());
	}
	
	private void updateDataSetEq(ArrayList<CBay> bay) {
		for(int i=0;i<bay.size();i++){
			CBay cBay = bay.get(i);
			for(int j=0;j<cBay.getListEq().size();j++){
				cBay.getListEq().get(j).setZoneIndex(cBay.getZone());
				EQ eq = dataSet.getM_htEq().get(cBay.getListEq().get(j).getDeviceName());				
				eq.setZoneIndex(cBay.getZone());
//				eq.setM_isBayUpperdirection(cBay.isM_isBayUpperdirection());
//				eq.setM_isEQUpperDirection(cBay.isM_isEQUpperDirection());
			}
		}
	}

	/**
	 * 
	 * @author : kyveri.kim
	 * @version : 2010. 07. 19 오전 9:28:39
	 * @date : 2010. 07. 19
	 * @변경이력 :
	 * @Method 설명 : 지정한 세대수 만큼 반복한 이후에 결과를 출력함.
	 */
	void finishArea()
	{
		elitism();
//		swap();
		if(CDataSet.DEBUG){
			for (int i = 0; i < lstPopulations.size(); i++)
			{
	//			System.out.print("\n"+i + "th: " + lstPopulations.get(i).getFitnessByArea());
				System.out.print("\n"+i + "th: " + lstPopulations.get(i).getFitnessByArea());
				for (int j = 0; j < lstPopulations.get(i).getChromosomeByInt().size(); j++)
				{
					System.out.print(" " + lstPopulations.get(i).getChromosomeByInt().get(j));
				}
			}
			System.out.println();
		}
		// resultSet에 결과 저장
		resultSet.getM_lstBayOrder().addAll(lstPopulations.get(0).getM_lstBay());
		// 각 eq에 저장된 zone 업데이트
		updateDataSetEq(lstPopulations.get(0).getM_lstBay());
	}
	/**
	 * 
	 * @author : kyveri.kim
	 * @version : 2010. 07. 15 오후 1:39:29
	 * @date : 2010. 07. 15
	 * @return
	 * @변경이력 :
	 * @Method 설명 : 교차연산을 수행하기 위해 필요한 부모 선택, Roulette Wheel Selection을 했으나 추후에 수정 가능.
	 */
	private int getRandomPopulation()
	{
		float sumOfFitnessReciprocal = 0; // fitness 함수의 역수로 계산
		for (int i = 0; i < lstPopulations.size(); i++) {
			if(lstPopulations.get(i).getIncrementalFitnessByDistance()!=0)
				sumOfFitnessReciprocal += (float)1/lstPopulations.get(i).getIncrementalFitnessByDistance();
		}
		Random r = new Random();
		float point = r.nextFloat() * sumOfFitnessReciprocal;
		float sum = 0;
		for (int i = 0; i < lstPopulations.size(); i++) {
			sum += (float)1/lstPopulations.get(i).getIncrementalFitnessByDistance();
			if(point < sum) return i;
		}
//		return -1;
		// fitnessfunction이 모두 0이어서 값을 구할 수 없는 경우 랜덤으로 리턴.
		return (int) (Math.random() * lstPopulations.size());
	}
	/**
	 * 
	 * @author : kyveri.kim
	 * @version : 2010. 07. 15 오후 1:39:29
	 * @date : 2010. 07. 15
	 * @return
	 * @변경이력 :
	 * @Method 설명 : 교차연산을 수행하기 위해 필요한 부모 선택, Roulette Wheel Selection을 했으나 추후에 수정 가능.
	 */
	private int getRandomPopulationArea()
	{
		float sumOfFitnessReciprocal = 0; // fitness 함수의 역수로 계산
		for (int i = 0; i < lstPopulations.size(); i++) {
			if(lstPopulations.get(i).getFitnessByArea()!=0)
				sumOfFitnessReciprocal += (float)1/lstPopulations.get(i).getFitnessByArea();
		}
		Random r = new Random();
		float point = r.nextFloat() * sumOfFitnessReciprocal;
		float sum = 0;
		for (int i = 0; i < lstPopulations.size(); i++) {
			sum += (float)1/lstPopulations.get(i).getFitnessByArea();
			if(point<sum) return i;
		}
		// fitnessfunction이 모두 0이어서 값을 구할 수 없는 경우 랜덤으로 리턴.
		return (int) (Math.random() * lstPopulations.size());
	}
	/**
	 * 
	 * @author : kyveri.kim
	 * @version : 2010. 07. 15 오후 1:39:11
	 * @date : 2010. 07. 15
	 * @param var1
	 * @param var2
	 * @return
	 * @변경이력 :
	 * @Method 설명 : Order Crossover 변형함. 두 부모 해를 통해 새로운 해 생성
	 */
	private MicroOrganism crossover(int var1, int var2)
	{
		// solution size 내에서 자름선이 될 index 두 개를 난수로 결정
		// 두 난수 중 작은 수와 큰 수 구분
		int chromosomeSize = lstPopulations.get(var1).getChromosomeByInt().size();
		Random r = new Random();
		int cut1 = r.nextInt(chromosomeSize / 2);
		int cut2 = r.nextInt(chromosomeSize / 2) + chromosomeSize / 2;
		
		int moduleNum = room.getM_lstModule().size(); // 모듈의 수
		int[] tempModuleEqNum = new int[moduleNum]; // 모듈별 설비들의 수
		for (int i = 0; i < moduleNum; i++)
		{
			tempModuleEqNum[i] = moduleEqNum[i];
		}
		
		// 자식 개체
		ArrayList<Integer> chromosomeByInt = new ArrayList<Integer>(chromosomeSize);
		ArrayList<EQ> chromosomeByName = new ArrayList<EQ>(chromosomeSize);
		
		for (int i = 0; i < chromosomeSize; i++)
		{
			chromosomeByInt.add(moduleNum + 3);
		}
		// 고른 두 자름선 사이의 값들은 첫번째 부모에서 상속
		for (int i = cut1; i < cut2; i++)
		{
			int a = lstPopulations.get(var1).getChromosomeByInt().get(i);
			chromosomeByInt.set(i, a);
			tempModuleEqNum[a]--;
		}
		
		// 두번째 부모의 답을 하나씩 가져와, 두 자름선 중 큰 것부터 시작하여 끝에 도달하면 처음으로 와 이후를 채우는 방식으로
		// 자식 개체에 상속한다. 단 위에서 이미 상속된 값들을 배제한다
		// 모듈에 설비가 남아있는 만큼 차례대로 배치하되, 모듈의 설비가 모자라는 경우가 발생하면
		// 다른 모듈의 설비를 배치
		for (int i = 0; i < chromosomeSize; i++)
		{
			int a = lstPopulations.get(var2).getChromosomeByInt().get(i); // var2의 모든 값을 접근한다 (i가 0부터 size까지 이므로)

			if (tempModuleEqNum[a] > 0)
			{ // 값이 이미 상속된 값이 아니라면
				chromosomeByInt.set(cut2++, a); // 끝에서부터 하나씩 채워간다
				tempModuleEqNum[a]--;
			} else
			{ // 이미 배치가 끝난 Module이 발견되면, 다른 Module을 배치
				int k;
				do
				{
					k = new Random().nextInt(moduleNum);
				} while (tempModuleEqNum[k] <= 0);
				chromosomeByInt.set(cut2++, k);
				tempModuleEqNum[k]--;
			}
			// 끝에 도달하면 처음으로 오게한다
			if (cut2 == chromosomeSize) cut2 = 0;

			// 큰 자름선이 작은 자름선에 도착하면 모든 유전자가 채워진 것이므로 교차를 중지한다
			if (cut2 == cut1) break;
		}
		
		chromosomeByName = eqMappingToName(chromosomeByInt, chromosomeByName);

		return new MicroOrganism(chromosomeByInt, chromosomeByName, propertiesByInt);
	}

	/**
	 * 
	 * @author : kyveri.kim
	 * @version : 2010. 07. 15 오후 1:38:17
	 * @date : 2010. 07. 15
	 * @param chromosomeByInt
	 * @param chromosomeByName
	 * @return
	 * @변경이력 :
	 * @Method 설명 : 설비 배치 순서가 숫자Base로 이루어져 있음. 이를 설비 이름 순서대로 Mapping 하는 작업. Cross Over, Mutate, Initialize를 할 때 사용.
	 */
	private ArrayList<EQ> eqMappingToName(ArrayList<Integer> chromosomeByInt, ArrayList<EQ> chromosomeByName)
	{
		/* 2019.07.11 임대은 review
		 * 중복 코드 많음
		 * 모듈별로 설비 대수를 체크하면서 꺼내서 chromosomeByName을 만듦
		 * 랜덤하게 배치라고 했는데 랜덤하지 않음-->뒤에서 부터 꺼냄
		 */
		int EqNum = room.getM_lstEQ().size(); // 전체 설비들의 수
		int moduleNum = room.getM_lstModule().size(); // 모듈의 수
		int[] tempModuleEqNum = new int[moduleNum]; // 모듈별 설비수
		int eqIndex = 0;
		EQ eq = null;
		chromosomeByName.clear();
		int k = 0;

		for (int j = 0; j < moduleNum; j++)
			tempModuleEqNum[j] = moduleEqNum[j];

		for (int j = 0; j < EqNum; j++)
		{ // 모듈별 설비들의 수를 넘지 않는 한에서 랜덤하게 배치
			k = chromosomeByInt.get(j);
			eqIndex = tempModuleEqNum[k]--;

			eq = room.getM_lstModule().get(k).getM_lstEQ().get(eqIndex - 1);
			chromosomeByName.add(eq);
		}

		return chromosomeByName;
	}

	/**
	 * 
	 * @author : kyveri.kim
	 * @version : 2010. 07. 15 오후 1:38:17
	 * @date : 2010. 07. 15
	 * @param chromosomeByInt
	 * @param tempSolbyName
	 * @return
	 * @변경이력 :
	 * @Method 설명 : 설비 배치 순서가 숫자Base로 이루어져 있음. 이를 설비 이름 순서대로 Mapping 하는 작업. Cross Over, Mutate, Initialize를 할 때 사용.
	 */
	private ArrayList<Integer> eqMappingToInt(ArrayList<Integer> chromosomeByInt, ArrayList<EQ> chromosomeByName)
	{
		int eqIndex = 0;
		String eqName = null;
		chromosomeByInt.clear();

		for (int i = 0; i < chromosomeByName.size(); i++)
		{
//			eqName = chromosomeByName.get(i).getMName();
			eqName = chromosomeByName.get(i).getDeviceName();
			eqIndex = propertiesByName.get(eqName);
			chromosomeByInt.add(eqIndex);
		}
		return chromosomeByInt;
	}

	// calculateDistance 구버전 	
//	/**
//	 * @author jwon.cho
//	 * @param fromEQ
//	 * @param toRoom
//	 * @param totalDistance
//	 * @param freq
//	 * @return
//	 * @Method 설명: EQ-Room 거리 구함
//	 * zone이 같은 경우 Room의 양쪽 끝 중 가까운 쪽(X => 방향성에 따라)으로 나가서 갓길 또는 중앙통로를 지나 다시 들어간다고 가정
//	 * eq에서 레일 방향성에 따라 나갈 수 있는 갓길까지 나가고, 나간 직후부터 도착 room 중심까지의 거리를 더한다.
//	 */
//	private double calculateDistance2(EQ fromEQ, CRoom fromRoom, CRoom toRoom, double totalDistance, double freq) {
//		//subRoom이 두 개 있는 경우에 대한 로직 추가.. 3개 이상인 경우는 없을 것으로 가정 jwon.cho
//		
//		//fromEQ Zone의 배치 방향에 따라 다르게 계산해야 함.
//		double distance = 0;
////System.out.println("**************");		
////System.out.println("fromEQ / fromRoom / toRoom / freq / fromEQ.isM_isEQUpperDirection() / fromEQ.isM_isBayUpperdirection()");		
////System.out.println(fromEQ.getDeviceName() + " / " + fromRoom.getM_strName() + " / " + toRoom.getM_strName() + " / " + freq + " / " + fromEQ.isM_isEQUpperDirection() + " / " + fromEQ.isM_isBayUpperdirection());
//		if(toRoom.getM_lstSubRoom().size()<2){
//			// 설비에서 나가는 지점은 베이와 접하는 변의 중심 - 포트로 보고..
//			// cf) EQUpperDirection = true;   ┭┬  EQUpperDirection = false;  ┌─┐
//			//                                └─┘                          ┴─┴
//			double x1 = fromEQ.getCoordLeft() + fromEQ.getCoordWidth()/2;
//			double y1 = 0;
//			if(fromEQ.isM_isEQUpperDirection()) y1 = fromEQ.getCoordTop() + fromEQ.getCoordLength();
//			else y1 = fromEQ.getCoordTop();
//			int fromZoneIndex = fromEQ.getZoneIndex();
//			
//			double x2 = toRoom.getM_lstSubRoom().get(0).getLeft() + toRoom.getM_lstSubRoom().get(0).getWidth()/2;
//			double y2 = toRoom.getM_lstSubRoom().get(0).getTop() + toRoom.getM_lstSubRoom().get(0).getHeight()/2;
//			int toZoneIndex = toRoom.getM_lstSubRoom().get(0).getM_nZoneIndex();
////System.out.println("fromZoneIndex / toZoneIndex");					
////System.out.println(fromZoneIndex + " / " + toZoneIndex);
//			
//			//zone이 같으므로 fromRoomLeft와 toRoomLeft는 같은 값이지만, 의미상 구분
//			double fromRoomLeft = fromRoom.getM_lstSubRoom().get(0).getLeft();
//			double fromRoomRight = fromRoom.getM_lstSubRoom().get(0).getLeft() + fromRoom.getM_lstSubRoom().get(0).getWidth();
//			
//			double toRoomLeft = toRoom.getM_lstSubRoom().get(0).getLeft();
//			double toRoomRight = toRoom.getM_lstSubRoom().get(0).getLeft() + toRoom.getM_lstSubRoom().get(0).getWidth();
//			if(fromZoneIndex == toZoneIndex){ ////////////////////// ZONE이 같은 경우
////System.out.println("fromZoneIndex == toZoneIndex");		
//				// bay 내의 레일이 시계 방향으로 돌아간다고 하자
//				// 베이가 설비 위쪽에 있으면 왼쪽 갓길로 나가고, 베이가 설비 아래쪽에 있으면 오른쪽 갓길로 나가야 한다.
//				   
//				double distanceX = 0;
//				if(dataSet.IS_RAIL_IN_BAY_CLOCKWISE){
//					if(fromEQ.isM_isEQUpperDirection()) distanceX = Math.abs(x1-fromRoomLeft) + Math.abs(toRoomLeft-x2);	
//					else distanceX = Math.abs(x1-fromRoomRight) + Math.abs(toRoomRight-x2);
//				} else {
//					if(!fromEQ.isM_isEQUpperDirection()) distanceX = Math.abs(x1-fromRoomLeft) + Math.abs(toRoomLeft-x2);	
//					else distanceX = Math.abs(x1-fromRoomRight) + Math.abs(toRoomRight-x2);
//				}
////System.out.println("************** 1 ");				
////System.out.println("x1:" + x1);					
////System.out.println("y1:" + y1);
////System.out.println("fromRoomLeft:" + fromRoomLeft);
////System.out.println("fromRoomRight:" + fromRoomRight);
////System.out.println("x2:" + x2);
////System.out.println("y2:" + y2);
////System.out.println("toRoomLeft:" + toRoomLeft);
////System.out.println("toRoomRight:" + toRoomRight);
////System.out.println("distanceX:" + distanceX);
////System.out.println("freq:" + freq);
//
//
////System.out.println("distanceX / x1 / fromRoomLeft / fromRoomRight / x2 / toRoomLeft / toRoomRight");				
////System.out.println(distanceX + " / " + x1 + " / " + fromRoomLeft + " / " + fromRoomRight + " / " + x2 + " / " + toRoomLeft + " / " + toRoomRight);
//				distance  = freq * (distanceX+Math.abs(y1-y2) * CDataSet.MICRO_ROOM_PENALTY);
//				totalDistance  += freq * (distanceX+Math.abs(y1-y2) * CDataSet.MICRO_ROOM_PENALTY);
//System.out.println("distance:" + distance);				
//				
////System.out.println("freq * (distanceX+Math.abs(y1-y2) / Math.abs(y1-y2) / y1 / y2");				
////System.out.println((freq * (distanceX+Math.abs(y1-y2)))  + " / " +  Math.abs(y1-y2)  + " / " +  y1  + " / " +  y2);
//
//			} else {
//System.out.println("fromZoneIndex != toZoneIndex");
////distance = freq * (Math.abs(x1-x2)+Math.abs(y1-y2) * CDataSet.MICRO_ZONE_PENALTY); //
//				// bay 내의 레일이 시계 방향으로 돌아간다고 하자
//				// 베이가 설비 위쪽에 있으면 왼쪽 갓길로 나간 다음에 다른 존으로 가고, 베이가 설비 아래쪽에 있으면 오른쪽 갓길로 나간 다음에 다른 존으로 간다.
//				// 설비에서 갓길까지의 거리를 구하고 갓길에 나간 순간부터 다른 존의 룸까지의 거리를 더한다.
//				double distanceX = 0;
//				if(dataSet.IS_RAIL_IN_BAY_CLOCKWISE){
//					if(fromEQ.isM_isEQUpperDirection()) distanceX = Math.abs(x1 - fromRoomLeft) + Math.abs(fromRoomLeft - x2)+Math.abs(y1 - y2);
//					else distanceX = Math.abs(x1-fromRoomRight) +  + Math.abs(fromRoomRight - x2)+Math.abs(y1 - y2);
//				} else {
//					if(!fromEQ.isM_isEQUpperDirection()) distanceX = Math.abs(x1-fromRoomRight) +  + Math.abs(fromRoomRight - x2)+Math.abs(y1 - y2);	
//					else distanceX = Math.abs(x1 - fromRoomLeft) + Math.abs(fromRoomLeft - x2)+Math.abs(y1 - y2);
//				}
//				totalDistance += freq * distanceX * CDataSet.MICRO_ZONE_PENALTY; //
//				
//				System.out.println("************** 2 ");
//				System.out.println("x1:" + x1);					
//				System.out.println("y1:" + y1);
//				System.out.println("fromRoomLeft:" + fromRoomLeft);
//				System.out.println("fromRoomRight:" + fromRoomRight);
//				System.out.println("x2:" + x2);
//				System.out.println("y2:" + y2);
//				System.out.println("toRoomLeft:" + toRoomLeft);
//				System.out.println("toRoomRight:" + toRoomRight);
//				System.out.println("distanceX:" + distanceX);
//				System.out.println("freq:" + freq);
//				System.out.println("distance:" + distanceX);	
////System.out.println("freq * (Math.abs(x1-x2)+Math.abs(y1-y2) / Math.abs(x1-x2) / x1 / x2 / Math.abs(y1-y2) / y1 / y2");				
////System.out.println((freq * (Math.abs(x1-x2)+Math.abs(y1-y2)))  + " / " +  Math.abs(x1-x2)  + " / " +  x1  + " / " +  x2  + " / " +  Math.abs(y1-y2)  + " / " +  y1  + " / " +  y2);
//			}
//		} else {
//			// 설비에서 나가는 지점은 베이와 접하는 변의 중심 - 포트로 보고..
//			// cf) EQUpperDirection = true;   ┭┬  EQUpperDirection = false;  ┌─┐
//			//                                └─┘                          ┴─┴
//			double x1 = fromEQ.getCoordLeft() + fromEQ.getCoordWidth()/2;
//			double y1 = 0;
//			if(fromEQ.isM_isEQUpperDirection()) y1 = fromEQ.getCoordTop() + fromEQ.getCoordLength();
//			else y1 = fromEQ.getCoordTop();
//			int fromZoneIndex = fromEQ.getZoneIndex();
//			
//			double x2_1 = toRoom.getM_lstSubRoom().get(0).getLeft() + toRoom.getM_lstSubRoom().get(0).getWidth()/2;
//			double y2_1 = toRoom.getM_lstSubRoom().get(0).getTop() + toRoom.getM_lstSubRoom().get(0).getHeight()/2;
//			double area_1 = toRoom.getM_lstSubRoom().get(0).getWidth() * toRoom.getM_lstSubRoom().get(0).getHeight();
//			int toZoneIndex_1 = toRoom.getM_lstSubRoom().get(0).getM_nZoneIndex();
//			
//			double x2_2 = toRoom.getM_lstSubRoom().get(1).getLeft() + toRoom.getM_lstSubRoom().get(0).getWidth()/2;
//			double y2_2 = toRoom.getM_lstSubRoom().get(1).getTop() + toRoom.getM_lstSubRoom().get(0).getHeight()/2;
//			double area_2 = toRoom.getM_lstSubRoom().get(1).getWidth() * toRoom.getM_lstSubRoom().get(1).getHeight();
//			int toZoneIndex_2 = toRoom.getM_lstSubRoom().get(1).getM_nZoneIndex();
//			
//			if(area_1 + area_2!=0){
//				//zone이 같으면 위와 같은 방식으로 계산하고, zone이 다르면 x값 차이의 절대값으로 계산한다.
//				double fromRoomLeft_1 = fromRoom.getM_lstSubRoom().get(0).getLeft();
//				double fromRoomRight_1 = fromRoom.getM_lstSubRoom().get(0).getLeft() + fromRoom.getM_lstSubRoom().get(0).getWidth();
//				
//				double toRoomLeft_1 = toRoom.getM_lstSubRoom().get(0).getLeft();
//				double toRoomRight_1 = toRoom.getM_lstSubRoom().get(0).getLeft() + toRoom.getM_lstSubRoom().get(0).getWidth();
//				if(fromZoneIndex == toZoneIndex_1){
//					
//					// bay 내의 레일이 시계 방향으로 돌아간다고 하자
//					// 베이가 설비 위쪽에 있으면 왼쪽 갓길로 나가고, 베이가 설비 아래쪽에 있으면 오른쪽 갓길로 나가야 한다.
//					double distanceX_1 = 0;
//					if(dataSet.IS_RAIL_IN_BAY_CLOCKWISE){
//						if(fromEQ.isM_isEQUpperDirection()) distanceX_1 = Math.abs(x1-fromRoomLeft_1) + Math.abs(toRoomLeft_1-x2_1);	
//						else distanceX_1 = Math.abs(x1-fromRoomRight_1) + Math.abs(toRoomRight_1-x2_1);
//					} else {
//						if(fromEQ.isM_isEQUpperDirection()) distanceX_1 = Math.abs(x1-fromRoomLeft_1) + Math.abs(toRoomLeft_1-x2_1);	
//						else distanceX_1 = Math.abs(x1-fromRoomRight_1) + Math.abs(toRoomRight_1-x2_1);
//					}
////					double distanceX_1 = Math.min(Math.abs(x1-fromRoomLeft_1) + Math.abs(toRoomLeft_1-x2_1), Math.abs(x1-fromRoomRight_1) + Math.abs(toRoomRight_1-x2_1));
//					
//					distance  = freq * (distanceX_1+Math.abs(y1-y2_1) * CDataSet.MICRO_ROOM_PENALTY);
//					totalDistance  += freq * (distanceX_1+Math.abs(y1-y2_1) * CDataSet.MICRO_ROOM_PENALTY); 
//					
//					System.out.println("************** 3 ");
//					System.out.println("x1:" + x1);					
//					System.out.println("y1:" + y1);
//					System.out.println("fromRoomLeft_1:" + fromRoomLeft_1);
//					System.out.println("fromRoomRight_1:" + fromRoomRight_1);
//					System.out.println("x2_1:" + x2_1);
//					System.out.println("y2_1:" + y2_1);
//					System.out.println("toRoomLeft_1:" + toRoomLeft_1);
//					System.out.println("toRoomRight_1:" + toRoomRight_1);
//					System.out.println("distanceX_1:" + distanceX_1);
//					System.out.println("freq:" + freq);
//					System.out.println("distance_1:" + distanceX_1);
//				} else {
////					distance += freq * (Math.abs(x1-x2_1)+Math.abs(y1-y2_1) * CDataSet.MICRO_ZONE_PENALTY) * (area_1/(area_1 + area_2)); //
//					// bay 내의 레일이 시계 방향으로 돌아간다고 하자
//					// 베이가 설비 위쪽에 있으면 왼쪽 갓길로 나간 다음에 다른 존으로 가고, 베이가 설비 아래쪽에 있으면 오른쪽 갓길로 나간 다음에 다른 존으로 간다.
//					// 설비에서 갓길까지의 거리를 구하고 갓길에 나간 순간부터 다른 존의 룸까지의 거리를 더한다.
//					double distanceX = 0;
//					if(dataSet.IS_RAIL_IN_BAY_CLOCKWISE){
//						if(fromEQ.isM_isEQUpperDirection()) distanceX = Math.abs(x1 - fromRoomLeft_1) + Math.abs(fromRoomLeft_1 - x2_1)+Math.abs(y1 - y2_1);
//						else distanceX = Math.abs(x1-fromRoomRight_1) +  + Math.abs(fromRoomRight_1 - x2_1)+Math.abs(y1 - y2_1);
//					} else {
//						if(!fromEQ.isM_isEQUpperDirection()) distanceX = Math.abs(x1-fromRoomRight_1) +  + Math.abs(fromRoomRight_1 - x2_1)+Math.abs(y1 - y2_1);	
//						else distanceX = Math.abs(x1 - fromRoomLeft_1) + Math.abs(fromRoomLeft_1 - x2_1)+Math.abs(y1 - y2_1);
//					}
//					totalDistance += freq * distanceX * CDataSet.MICRO_ZONE_PENALTY * (area_1/(area_1 + area_2)); // 
//					
//					System.out.println("************** 4 ");
//					System.out.println("x1:" + x1);					
//					System.out.println("y1:" + y1);
//					System.out.println("fromRoomLeft_1:" + fromRoomLeft_1);
//					System.out.println("fromRoomRight_1:" + fromRoomRight_1);
//					System.out.println("x2_1:" + x2_1);
//					System.out.println("y2_1:" + y2_1);
//					System.out.println("toRoomLeft_1:" + toRoomLeft_1);
//					System.out.println("toRoomRight_1:" + toRoomRight_1);
//					System.out.println("distanceX:" + distanceX);
//					System.out.println("freq:" + freq);
//					System.out.println("distance:" + distanceX);
//				}	
//				double fromRoomLeft_2 = fromRoom.getM_lstSubRoom().get(0).getLeft();
//				double fromRoomRight_2 = fromRoom.getM_lstSubRoom().get(0).getLeft() + fromRoom.getM_lstSubRoom().get(0).getWidth();
//				
//				double toRoomLeft_2 = toRoom.getM_lstSubRoom().get(1).getLeft();
//				double toRoomRight_2 = toRoom.getM_lstSubRoom().get(1).getLeft() + toRoom.getM_lstSubRoom().get(1).getWidth();
//				if(fromZoneIndex == toZoneIndex_2){
//					// bay 내의 레일이 시계 방향으로 돌아간다고 하자
//					// 베이가 설비 위쪽에 있으면 왼쪽 갓길로 나가고, 베이가 설비 아래쪽에 있으면 오른쪽 갓길로 나가야 한다.
//					double distanceX_2 = 0;
//					if(dataSet.IS_RAIL_IN_BAY_CLOCKWISE){
//						if(fromEQ.isM_isEQUpperDirection()) distanceX_2 = Math.abs(x1-fromRoomLeft_2) + Math.abs(toRoomLeft_2-x2_2);	
//						else distanceX_2 = Math.abs(x1-fromRoomRight_2) + Math.abs(toRoomRight_2-x2_2);
//					} else {
//						if(fromEQ.isM_isEQUpperDirection()) distanceX_2 = Math.abs(x1-fromRoomLeft_2) + Math.abs(toRoomLeft_2-x2_2);	
//						else distanceX_2 = Math.abs(x1-fromRoomRight_2) + Math.abs(toRoomRight_2-x2_2);
//					}
//					
//					System.out.println("************** 5 ");
//					System.out.println("x1:" + x1);					
//					System.out.println("y1:" + y1);
//					System.out.println("fromRoomLeft_2:" + fromRoomLeft_2);
//					System.out.println("fromRoomRight_2:" + fromRoomRight_2);
//					System.out.println("x2_2:" + x2_2);
//					System.out.println("y2_2:" + y2_2);
//					System.out.println("toRoomLeft_2:" + toRoomLeft_2);
//					System.out.println("toRoomRight_2:" + toRoomRight_2);
//					System.out.println("distanceX_2:" + distanceX_2);
//					System.out.println("freq:" + freq);
//					System.out.println("distance_2:" + distanceX_2);
////					double distanceX_2 = Math.min(Math.abs(x1-fromRoomLeft_2) + Math.abs(toRoomLeft_2-x2_2), Math.abs(x1-fromRoomRight_2) + Math.abs(toRoomRight_2-x2_2));
////					distance  += freq * (distanceX_2+Math.abs(y1-y2_2) * CDataSet.MICRO_ROOM_PENALTY);
//					totalDistance  += freq * (distanceX_2+Math.abs(y1-y2_2) * CDataSet.MICRO_ROOM_PENALTY); 
//				} else {
////					distance += freq * (Math.abs(x1-x2_2)+Math.abs(y1-y2_2) * CDataSet.MICRO_ZONE_PENALTY) * (area_2/(area_1 + area_2)); //
//					double distanceX = 0;
//					if(dataSet.IS_RAIL_IN_BAY_CLOCKWISE){
//						if(fromEQ.isM_isEQUpperDirection()) distanceX = Math.abs(x1 - fromRoomLeft_2) + Math.abs(fromRoomLeft_2 - x2_2)+Math.abs(y1 - y2_2);
//						else distanceX = Math.abs(x1-fromRoomRight_2) +  + Math.abs(fromRoomRight_2 - x2_2)+Math.abs(y1 - y2_2);
//					} else {
//						if(!fromEQ.isM_isEQUpperDirection()) distanceX = Math.abs(x1-fromRoomRight_2) +  + Math.abs(fromRoomRight_2 - x2_2)+Math.abs(y1 - y2_2);	
//						else distanceX = Math.abs(x1 - fromRoomLeft_2) + Math.abs(fromRoomLeft_2 - x2_2)+Math.abs(y1 - y2_2);
//					}
//					totalDistance += freq * distanceX * CDataSet.MICRO_ZONE_PENALTY * (area_2/(area_1 + area_2)); //
//					
//					System.out.println("************** 6 ");
//					System.out.println("x1:" + x1);					
//					System.out.println("y1:" + y1);
//					System.out.println("fromRoomLeft_2:" + fromRoomLeft_2);
//					System.out.println("fromRoomRight_2:" + fromRoomRight_2);
//					System.out.println("x2_2:" + x2_2);
//					System.out.println("y2_2:" + y2_2);
//					System.out.println("toRoomLeft_2:" + toRoomLeft_2);
//					System.out.println("toRoomRight_2:" + toRoomRight_2);
//					System.out.println("distanceX:" + distanceX);
//					System.out.println("freq:" + freq);
//					System.out.println("distance:" +distanceX);
//				}
//			}
//		}
////		System.out.printlndistance:"+distance);
//		return totalDistance;
//	}
}
